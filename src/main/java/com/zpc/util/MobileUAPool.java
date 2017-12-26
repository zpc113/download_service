package com.zpc.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Created by 和谐社会人人有责 on 2017/12/9.
 * 移动端UA池
 */
public class MobileUAPool {

    public static final List<String> UAS = new ArrayList<String>();

    /**
     * 随机取一个UA
     * @return
     */
    public static String getUA() {
        int index = new Random().nextInt(UAS.size());
        return UAS.get(index);
    }

    static {
        UAS.add("");
    }

}
