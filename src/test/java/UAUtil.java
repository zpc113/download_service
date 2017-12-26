import java.io.*;

/**
 * Created by 和谐社会人人有责 on 2017/12/9.
 */
public class UAUtil {

    public static void main(String[] args) throws Exception {

        BufferedReader bufferedReader = new BufferedReader(new FileReader(new File("uas.txt")));
        BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter("uas_new.txt"));
        while (bufferedReader.readLine() != null) {
            String temp = bufferedReader.readLine();
            temp = "UAS.add(\"" + temp + "\");\n";
            bufferedWriter.write(temp);
        }
//        bufferedReader.close();
        bufferedWriter.flush();
//        bufferedWriter.close();
    }

}
