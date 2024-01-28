import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

public class WordCountRunnable implements Runnable {
    private final String word;
    private final String filePath;
    private volatile String result;

    public WordCountRunnable(String word, String filePath) {
        this.word = word;
        this.filePath = filePath;
    }

    @Override
    public void run() {
        int count = 0;
        File f = new File(filePath);
        try {
            Scanner scanner = new Scanner(f);
            while (scanner.hasNext()) {
                String str = scanner.next();
                if (str.equals(word)) {
                    count++;
                }
            }

            result = word + " appears: " + count + " times\n";

            scanner.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public String getResult() {
        return result == null? "" : result;
    }
}