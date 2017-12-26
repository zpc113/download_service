package com.zpc.service;

import com.zpc.dto.OrderMessage;
import com.zpc.dto.Request;

/**
 * Created by 和谐社会人人有责 on 2017/12/7.
 */
public interface DownloadService {

    /**
     * 下载种子页面
     * @param orderMessage
     */
    public void download(OrderMessage orderMessage);

}
