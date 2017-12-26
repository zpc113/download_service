package com.zpc.listener;

import com.zpc.dto.ControlExecutorOrder;
import com.zpc.dto.OrderMessage;
import com.zpc.dto.Request;
import com.zpc.dto.RoutingKey;
import com.zpc.executors.ExecutorsPool;
import com.zpc.send.SendMessage;
import com.zpc.service.DownloadService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by 和谐社会人人有责 on 2017/11/29.
 */
@Component
public class MessageReceiver implements MessageListener {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private ExecutorsPool executorsPool;
    @Autowired
    private DownloadService downloadService;
    @Autowired
    private SendMessage sendMessage;

    public void onMessage(Message message) {
        try {
            // 反序列化，获得对应的对象
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(message.getBody());
            ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream);
            final OrderMessage orderMessage = (OrderMessage) objectInputStream.readObject();

            String order = orderMessage.getOrder();
            if (ControlExecutorOrder.SEED.equals(order)) {
                // 下载种子页面
                downloadService.download(orderMessage);
            } else if (ControlExecutorOrder.NEW.equals(order)) {
                // 初始化线程池
                ExecutorService executorService = Executors.newFixedThreadPool(5);
                executorsPool.getExecutors().put(orderMessage.getContainerName() , executorService);
                // 通知队列服务器线程池已初始化完成
                OrderMessage orderMessageToQueue = new OrderMessage();
                orderMessageToQueue.setOrder(ControlExecutorOrder.READY);
                orderMessageToQueue.setContainerName(orderMessage.getContainerName().split("_scheduleId_")[0]);
                sendMessage.sendQueueMessage(orderMessageToQueue , RoutingKey.QUEUE_ROUTINGKEY);
            } else if (ControlExecutorOrder.DOWNLOAD.equals(order)) {
                // 获取页面并下载
                RestTemplate restTemplate = new RestTemplate();
                List<Request> requests = restTemplate.getForObject("http://localhost:8080/queue/{queueName}/get" ,
                        List.class , orderMessage.getContainerName());
                ExecutorService executorService = executorsPool.getExecutors().get(orderMessage.getContainerName());
                for (final Request request : requests) {
                    executorService.execute(new Runnable() {
                        public void run() {
                            OrderMessage orderMessageDownload = new OrderMessage();
                            orderMessageDownload.setContainerName(orderMessage.getContainerName());
                            orderMessageDownload.setRequest(request);
                            downloadService.download(orderMessageDownload);
                        }
                    });
                }
                // 通知解析服务器从数据库中获取页面进行解析
                OrderMessage orderMessageToParse = new OrderMessage();
                orderMessageToParse.setContainerName(orderMessage.getContainerName());
                orderMessageToParse.setOrder(ControlExecutorOrder.PARSE);
                sendMessage.sendParseMessage(orderMessageToParse , RoutingKey.PARSESERVICE_ROUTINGKEY);
            }else if (ControlExecutorOrder.DESTROY.equals(order)) {
                // 收到暂停或停止指令，销毁线程池
                String containerName = orderMessage.getContainerName();
                executorsPool.getExecutors().get(containerName).shutdownNow();
                executorsPool.getExecutors().remove(containerName);
                logger.info("线程池已销毁" + containerName);
            }
        } catch (Exception e) {
            logger.error(e.getMessage() , "接收消息失败");
        }


    }
}
