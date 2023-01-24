import java.io.*;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.regex.Pattern;

public class MultithreadedWordCount
{
    private static BlockingQueue<String> sharedQueue = new LinkedBlockingQueue<>(1000);
    private static Thread[] consumers;
    private static Map<String, Integer> wordCounts = new ConcurrentHashMap<String, Integer>();
    private static boolean readingFinished = false;
    private String filePath;
    private String outputFile;
    private int nbThreads;

    //Pattern to match all non-ascii letters to be removed.
    private static Pattern specialCharsRemovePattern = Pattern.compile("[^a-zA-Z]");

    public MultithreadedWordCount(String filePath, String outputFile, int nbThreads)
    {
        this.filePath = filePath;
        this.outputFile = outputFile;
        this.nbThreads = nbThreads;
    }

    public void CountWords()  throws InterruptedException
    {
        System.out.printf("Execution starting with %d consumer thread(s) ...\n", nbThreads);
        long executionStartTime = System.currentTimeMillis();

        consumers = new Thread[nbThreads];

        Thread producer = new Thread(new Producer(filePath));
        producer.start();

        for (int i = 0; i < nbThreads; i++)
        {
            consumers[i] = new Thread(new Consumer());
            consumers[i].start();
        }

        producer.join();
        for (int i = 0; i < nbThreads; i++)
        {
            consumers[i].join();
        }

        System.out.printf("Word Counting took %d ms.\n", System.currentTimeMillis() - executionStartTime);
        System.out.printf("Now ordering results ...\n");

        Map<String, Integer> ordered = new TreeMap<String, Integer>(wordCounts);
        try
        (
                FileWriter fstream = new FileWriter(outputFile);
                BufferedWriter out = new BufferedWriter(fstream);
        )
        {
            for (Entry<String, Integer> entry : ordered.entrySet())
            {
                out.write(String.format("%s %s\n", entry.getKey(), entry.getValue()));
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        System.out.printf("Total Execution took %d ms.\n", System.currentTimeMillis() - executionStartTime);
    }

    public static class Producer implements Runnable
    {
        private String inputFile;
        public Producer(String inputFile)
        {
            this.inputFile = inputFile;
        }

        @Override
        public void run()
        {
            File input = new File(inputFile);
            int count = 0;
            try (BufferedReader br = new BufferedReader(new FileReader(input));)
            {
                String line;
                while ((line = br.readLine()) != null)
                {
                    sharedQueue.put(line);
                    count++;
                    if (count % 1000000 == 0)
                    {
                        System.out.printf("%dM lines read from input. Current Queue size : %d\n", count / 1000000, sharedQueue.size());
                    }
                }
            }
            catch (FileNotFoundException e)
            {
                e.printStackTrace();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
            }
            readingFinished = true;
        }
    }

    public static class Consumer implements Runnable
    {
        @Override
        public void run()
        {
            while (!readingFinished || !sharedQueue.isEmpty())
            {
                // Get a line from the queue
                String line = sharedQueue.poll();
                if (line == null) continue;

                // Tokenize the line and do some word counting
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
