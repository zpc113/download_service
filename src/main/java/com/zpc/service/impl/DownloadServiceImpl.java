package com.zpc.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.zpc.dao.controlcenter.TaskInfoDao;
import com.zpc.dao.page.PageDao;
import com.zpc.dao.controlcenter.ScheduleDao;
import com.zpc.dao.page.PageTableDao;
import com.zpc.dto.*;
import com.zpc.entity.controlcenter.Schedule;
import com.zpc.send.SendMessage;
import com.zpc.service.DownloadService;
import com.zpc.util.DesktopUAPool;
import com.zpc.util.MobileUAPool;
import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import java.io.IOException;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by 和谐社会人人有责 on 2017/12/7.
 */
@Service
public class DownloadServiceImpl implements DownloadService{
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private ScheduleDao scheduleDao;
    @Autowired
    private PageTableDao pageTableDao;
    @Autowired
    private PageDao pageDao;
    @Autowired
    private SendMessage sendMessage;
    @Autowired
    private TaskInfoDao taskInfoDao;

    /**
     * 下载页面
     * @param orderMessage
     */
    public void download(OrderMessage orderMessage) {
        Request request = orderMessage.getRequest();
        // 配置连接信息
        Connection connection = this.handleRequest(request);
        Document document = null;
        // 开始下载页面
        try {
            if (HttpRequestMethod.GET.equals(request.getMethod())) {
                document = connection.get();
            } else if (HttpRequestMethod.POST.equals(request.getMethod())) {
                Map<String , String> parameters = request.getParameters();
                connection.data(parameters);
                document = connection.post();
            }
            logger.info("下载页面成功 " + request.toString());
        } catch (HttpStatusException e) {
            logger.error(e.getMessage() , "下载页面失败  " + request.toString());
            // 处理异常码
            int statusCode = e.getStatusCode();
        } catch (IOException e) {
            logger.error(e.getMessage() , "下载页面失败  " + request.toString());
        }
        // 是否为第一次运行任务
        boolean isNew = ControlExecutorOrder.NEW.equals(orderMessage.getOrder());
        if (isNew) {
            // 新建page表，用来存放下载的页面
            pageTableDao.createTable(orderMessage.getContainerName());
        }

        long scheduleId = Long.parseLong(orderMessage.getContainerName().split("scheduleId_")[1]);
        Schedule schedule = scheduleDao.findById(scheduleId);
        schedule.setRequestNum(schedule.getRequestNum() + 1);

        // 页面入库 注意：此处拿到的错误页面也会入库，如403、503、500等
        // 页面先入库，然后再开始创建解析线程池
        PageDto page = new PageDto();
        page.setRequest(orderMessage.getRequest());
        page.setDocument(document);
        String pageString = JSONObject.toJSONString(page);
        pageDao.save(pageString , orderMessage.getContainerName());
        logger.info("页面入库 " + request.toString());

        if (document != null) {
            // 成功页面数加1
            schedule.setSuccessNum(schedule.getSuccessNum() + 1);
            // 如果是第一次下载页面，则通知解析服务器新建解析线程池
            if (isNew) {
                OrderMessage orderMessageToParse = new OrderMessage();
                orderMessage.setOrder(ControlExecutorOrder.SEED);
                orderMessage.setContainerName(orderMessage.getContainerName());
                sendMessage.sendParseMessage(orderMessageToParse , RoutingKey.PARSESERVICE_ROUTINGKEY);
            }
        } else {
            // 失败页面数加1
            schedule.setFailedNum(schedule.getFailedNum() + 1);
            // 如果是第一次运行任务，那么直接结束任务了，设置结束时间
            if (isNew) {
                Date now = new Date();
                schedule.setEndTime(now);
                logger.info("调度结束 " + orderMessage.toString());
                // 通知队列服务器销毁队列
                OrderMessage orderMessageToQueue = new OrderMessage();
                orderMessage.setOrder(TaskOrder.STOP);
                sendMessage.sendQueueMessage(orderMessageToQueue , RoutingKey.QUEUE_ROUTINGKEY);
                logger.info("通知销毁队列" + orderMessageToQueue.toString());
                // 操作数据库，设置任务运行状态为已完成
                long taskId = Long.parseLong(orderMessage.getContainerName().replace("QueueName" , "").split("_scheduleId_")[0]);
                taskInfoDao.updateRunStatus(taskId , 2);
            }
        }
        // 更新调度信息
        scheduleDao.update(schedule);
        logger.info("调度信息已更新 " + schedule.toString());
    }

    /**
     * 开始进行下载页面前的配置
     * @param request
     * @return
     */
    public Connection handleRequest(Request request) {

        String url = request.getUrl();
        Map<String , String> headers = request.getHeaders();
        String method = request.getMethod();

        Connection connection = Jsoup.connect(url);
        if (!CollectionUtils.isEmpty(headers)) {
            Iterator<Map.Entry<String , String>> entrys = headers.entrySet().iterator();
            while (entrys.hasNext()) {
                Map.Entry<String , String> entry = entrys.next();
                connection.header(entry.getKey() , entry.getValue());
            }
        }

        if (!headers.containsKey("User-Agent")) {
            // 从UA池中随机取一个UA
            // 应该添加移动端UA还是电脑端UA
            String ua = "";
            if (!request.isMobile()){
                ua = DesktopUAPool.getUA();
            } else {
                ua = MobileUAPool.getUA();
            }
            connection.header("User-Agent" , ua);
        }
        return connection;
    }

    public static void main(String[] args) throws IOException {
        String url = "http://www.hao6v.com/mj/2015-02-24/24863.html";
        Connection connection = Jsoup.connect(url);
        Document document = connection.get();
        Elements elements = document.getElementsByTag("table");

        for (Element element : elements) {
            Elements trs = element.getElementsByTag("tr");
            for (int i = 1 ; i < trs.size() ; i ++) {
                Element tr = trs.get(i);
                if (i == 0) continue;
                String href = tr.getElementsByTag("td").get(0).getElementsByTag("a").attr("href");
                System.out.println(href);
            }
        }
    }
















}
