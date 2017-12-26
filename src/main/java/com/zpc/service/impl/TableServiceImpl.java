package com.zpc.service.impl;

import com.zpc.dao.page.PageTableDao;
import com.zpc.service.TableService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Created by 和谐社会人人有责 on 2017/12/10.
 */
@Service
public class TableServiceImpl implements TableService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private PageTableDao pageTableDao;

    /**
     * 新建表
     * @param tableName
     */
    public void createTable(String tableName) {
        try {
            pageTableDao.createTable(tableName);
            logger.info("新建表成功 " + tableName);
        } catch (Exception e) {
            logger.error(e.getMessage() , "新建表失败 " + tableName);
        }
    }
}
