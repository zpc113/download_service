package com.zpc.dto;

import com.zpc.dto.Request;
import org.jsoup.nodes.Document;

/**
 * Created by 和谐社会人人有责 on 2017/12/10.
 */
public class PageDto {

    private Request request;

    private Document document;

    public Request getRequest() {
        return request;
    }

    public void setRequest(Request request) {
        this.request = request;
    }

    public Document getDocument() {
        return document;
    }

    public void setDocument(Document document) {
        this.document = document;
    }

}
