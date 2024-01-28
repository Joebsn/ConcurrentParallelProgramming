import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.regex.Pattern;

public class MultithreadedWordCount {
    private static final BlockingQueue<String> sharedQueue = new LinkedBlockingQueue<>(1000);
    private static Map<String, Integer> wordCounts;
    private static boolean readingFinished = false;
    private final String filePath;
    private final String outputFile;
    private final int nbThreads;
    private static final Pattern specialCharsRemovePattern = Pattern.compile("[^a-zA-Z]");

    public MultithreadedWordCount(String filePath, String outputFile, int nbThreads) {
        this.filePath = filePath;
        this.outputFile = outputFile;
        this.nbThreads = nbThreads;
        wordCounts = new ConcurrentHashMap<>();
    }

    public void countWords()  throws InterruptedException {
        System.out.printf("Execution starting with %d consumer thread(s) ...\n", nbThreads);
        long executionStartTime = System.currentTimeMillis();

        Thread[] consumers = new Thread[nbThreads];

        Thread producer = new Thread(new Producer(filePath));
        producer.start();

        for (int i = 0; i < nbThreads; i++) {
            consumers[i] = new Thread(new Consumer());
            consumers[i].start();
        }

        producer.join();
        for (int i = 0; i < nbThreads; i++) {
            consumers[i].join();
        }

        System.out.printf("Word Counting took %d ms.\n", System.currentTimeMillis() - executionStartTime);
        System.out.println("Now ordering results ...\n");

        Map<String, Integer> ordered = new TreeMap<>(wordCounts);
        try (FileWriter stream = new FileWriter(outputFile);
                BufferedWriter out = new BufferedWriter(stream)) {

            for (Entry<String, Integer> entry : ordered.entrySet()) {
                out.write(String.format("%s %s\n", entry.getKey(), entry.getValue()));
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        System.out.printf("Total Execution took %d ms.\n", System.currentTimeMillis() - executionStartTime);
    }

    private record Producer(String inputFile) implements Runnable {
        @Override
            public void run() {
                File input = new File(inputFile);
                int count = 0;
                try (BufferedReader br = new BufferedReader(new FileReader(input))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        sharedQueue.put(line);
                        count++;
                        if (count % 1000000 == 0) {
                            System.out.printf("%dM lines read from input. Current Queue size : %d\n", count / 1000000, sharedQueue.size());
                        }
                    }
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }
                readingFinished = true;
            }
        }

    private static class Consumer implements Runnable {
        @Override
        public void run() {
            while (!readingFinished || !sharedQueue.isEmpty()) {
                String line = sharedQueue.poll();
                if (line == null) {
                    continue;
                }

                String[] words = specialCharsRemovePattern.matcher(line)
                        .replaceAll(" ").toLowerCase().split("\\s+");

                for (String word : words) {
                    int count = wordCounts.containsKey(word) ? wordCounts.get(word) + 1 : 1;
                    wordCounts.put(word, count);
                }
            }
        }
    }
}
