package com.zpc.entity.page;

import java.util.Date;

/**
 * Created by 和谐社会人人有责 on 2017/12/10.
 */
public class Page {

    private long pageId;

    private String pageStr;

    private Date createTime;

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public long getPageId() {
        return pageId;
    }

    public void setPageId(long pageId) {
        this.pageId = pageId;
    }

    public String getPageStr() {
        return pageStr;
    }

    public void setPageStr(String pageStr) {
        this.pageStr = pageStr;
    }

    @Override
    public String toString() {
        return "Page{" +
                "pageId=" + pageId +
                ", pageStr='" + pageStr + '\'' +
                '}';
    }

    public Page() {
    }

    public Page(long pageId, String pageStr , Date createTime) {
        this.pageId = pageId;
        this.pageStr = pageStr;
        this.createTime = createTime;
    }
}
