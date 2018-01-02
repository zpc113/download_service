package com.zpc.listener;

import com.zpc.dao.page.PageTableDao;
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
    @Autowired
    private PageTableDao pageTableDao;

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
                this.download(orderMessage);
            } else if (ControlExecutorOrder.SUSPEND.equals(order)) {
                // 销毁线程池
                this.destory(orderMessage);
            } else if (ControlExecutorOrder.DESTROY.equals(order)) {
                // 销毁线程池
                this.destory(orderMessage);
                // 通知解析服务器销毁解析线程
                OrderMessage orderMessageToParse = new OrderMessage();
                orderMessageToParse.setOrder(ControlExecutorOrder.DESTROY);
                orderMessageToParse.setContainerName(orderMessage.getContainerName());
                try {
                    sendMessage.sendParseMessage(orderMessage , RoutingKey.PARSESERVICE_ROUTINGKEY);
                    logger.info("发送销毁线程池消息到解析服务器成功-->" + orderMessageToParse.toString());
                } catch (Exception e) {
                    logger.error(e.getMessage() , "发送销毁线程池消息到解析服务器失败-->" + orderMessageToParse.toString());
                }
            } else if (ControlExecutorOrder.RECOVER.equals(order)) {
                // 恢复线程池
                // 初始化线程池
                ExecutorService executorService = Executors.newFixedThreadPool(5);
                executorsPool.getExecutors().put(orderMessage.getContainerName() , executorService);
                // 通知队列服务器线程池已初始化完成
                OrderMessage orderMessageToQueue = new OrderMessage();
                orderMessageToQueue.setOrder(ControlExecutorOrder.RECOVER);
                orderMessageToQueue.setContainerName(orderMessage.getContainerName().split("_scheduleId_")[0]);
                sendMessage.sendQueueMessage(orderMessageToQueue , RoutingKey.QUEUE_ROUTINGKEY);
                // 休眠10秒钟，等待解析服务器创建解析线程池
                Thread.sleep(10000);
                // 开始下载页面
                this.download(orderMessage);
            }
        } catch (Exception e) {
            logger.error(e.getMessage() , "接收消息失败");
        }
    }

    /**
     * 销毁线程池公共方法
     * @param orderMessage
     */
    public void destory(OrderMessage orderMessage) {
        // 收到暂停或停止指令，销毁线程池
        String containerName = orderMessage.getContainerName();
        ExecutorService executorService = executorsPool.getExecutors().get(containerName);
        // 将线程池移除线程池管理器
        executorsPool.getExecutors().remove(containerName);
        // 停掉线程池
        executorService.shutdownNow();
        logger.info("线程池已销毁" + containerName);
    }

    /**
     * 下载页面公共方法
     * @param orderMessage
     */
    public void download(final OrderMessage orderMessage) {
        RestTemplate restTemplate = new RestTemplate();
        List<Request> requests = restTemplate.getForObject("http://localhost:8080/queue/{queueName}/get" ,
                List.class , orderMessage.getContainerName());
        if (requests == null || requests.size() == 0) {
            // 拿到的request list为空，此时应查询数据库判断该任务下载的页面是否都已解析完成，如果都已解析完成，则说明任务已完成
            // 如果没有解析完成，休眠10秒钟再次请求队列服务器获取request list
            String tableName = orderMessage.getContainerName();
            int tableSize = pageTableDao.getTableSize(tableName);
            if (tableSize == 0) {
                // 任务已完成，停掉线程池，通知解析服务器停掉线程池
                ExecutorService executorService = executorsPool.getExecutors().get(orderMessage.getContainerName());
                executorService.shutdown();
                OrderMessage orderMessageToParse = new OrderMessage();
                orderMessageToParse.setOrder(ControlExecutorOrder.COMPLETE);
                orderMessageToParse.setContainerName(orderMessage.getContainerName());
                try {
                    sendMessage.sendParseMessage(orderMessageToParse , RoutingKey.PARSESERVICE_ROUTINGKEY);
                    logger.info("发送完成指令到解析服务器成功-->" + orderMessageToParse.toString());
                } catch (Exception e) {
                    logger.error(e.getMessage() , "发送完成指令到解析服务器失败-->" + orderMessageToParse.toString());
                }
                return;
            } else {
                // 10秒钟后再次尝试获取请求
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                requests = restTemplate.getForObject("http://localhost:8080/queue/{queueName}/get" ,
                        List.class , orderMessage.getContainerName());
            }
        }
        ExecutorService executorService = executorsPool.getExecutors().get(orderMessage.getContainerName());
        if (executorService != null) {
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
        }
    }
}
