import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;

/**
 * Created by 和谐社会人人有责 on 2017/12/10.
 */
public class TestDownload {

    public static void main(String[] args){
        Connection connection = Jsoup.connect("http://www.hubei-safety.org/ax/template/hbsaqscjsxh/index.jsp/fdafsa");
        Document document = null;
        try {
            document = connection.get();
        } catch (HttpStatusException e){
            System.out.println(document.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println(document.toString());
    }

}
