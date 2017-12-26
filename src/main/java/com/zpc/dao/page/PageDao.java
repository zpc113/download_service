package com.zpc.dao.page;

import org.apache.ibatis.annotations.Param;

/**
 * Created by 和谐社会人人有责 on 2017/12/10.
 */
public interface PageDao {

    /**
     * 页面入库
     * @param page
     */
    void save(@Param("pageString") String pageString , @Param("tableName") String tableName);

}
