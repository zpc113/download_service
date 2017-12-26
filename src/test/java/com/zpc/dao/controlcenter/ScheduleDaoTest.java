package com.zpc.dao.controlcenter;

import com.zpc.entity.controlcenter.Schedule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.List;

import static org.junit.Assert.*;

/**
 * Created by 和谐社会人人有责 on 2017/12/10.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:spring/spring-dao.xml")
public class ScheduleDaoTest {
    @Autowired
    private ScheduleDao scheduleDao;

    @Test
    public void findAll() throws Exception {
        List<Schedule> schedules = scheduleDao.findAll(1001);
        for (Schedule schedule : schedules) {
            System.out.println("schedule-------------->" + schedule.toString());
        }
    }

}