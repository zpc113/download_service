package com.zpc.executors;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ExecutorService;

/**
 * Created by 和谐社会人人有责 on 2017/12/6.
 * 管理线程池
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
public class ExecutorsPool {

    private Map<String , ExecutorService> executors;

    public Map<String, ExecutorService> getExecutors() {
        return executors;
    }

    public void setExecutors(Map<String, ExecutorService> executors) {
        this.executors = executors;
    }
}
