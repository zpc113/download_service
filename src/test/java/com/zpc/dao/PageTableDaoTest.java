package com.zpc.dao;

import com.zpc.dao.page.PageTableDao;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Created by 和谐社会人人有责 on 2017/12/10.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:spring/spring-dao.xml")
public class PageTableDaoTest {

    @Autowired
    private PageTableDao pageTableDao;

    @Test
    public void createTable() throws Exception {
        pageTableDao.createTable("test_table_name4");
    }

}