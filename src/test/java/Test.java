import com.alibaba.fastjson.JSONObject;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by 和谐社会人人有责 on 2017/12/6.
 */
public class Test {

    public static void main(String[] args) throws Exception{

        final ExecutorService executorService = Executors.newFixedThreadPool(5);

        boolean b = true;
        int i = 0;
        while (b) {
            Runnable runnable = new Runnable() {
                public void run() {
                    System.out.println("线程正在运行");
                }
            };
            executorService.execute(runnable);
            Thread.sleep(2000);
            Thread.currentThread().join(2000);
        }
        System.out.println("毒丸对象执行");

        b = false;
        executorService.shutdownNow();

    }

}
