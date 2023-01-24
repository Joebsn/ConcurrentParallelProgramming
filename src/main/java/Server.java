import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.*;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Semaphore;

public class Server {
    private static final String filePath = FileSystems.getDefault().getPath("data.txt").toString();
    private static String fileName;
    private static int portNumber = 4000, len = 1024;
    private static ReaderWriter readerWriter;
    private static Socket socket = null;
    private static ServerSocket server = null;
    private static DataInputStream in = null;
    private int numReaders = 0;
    private Semaphore mutex = new Semaphore(1);
    private Semaphore lock = new Semaphore(1);

    public Server(String fileName) {

        this.fileName = fileName;
        readerWriter = new ReaderWriter();
    }

    public void StartServer() {
        DatagramPacket dataPacket, returnPacket;
        try {
            DatagramSocket datasocket = new DatagramSocket(portNumber);
            System.out.println("Starting Server");
            while (true) {
                try {
                    byte[] buf = new byte[len];
                    dataPacket = new DatagramPacket(buf, buf.length);
                    datasocket.receive(dataPacket);
                    String data = new String(dataPacket.getData(), 0, dataPacket.getLength());

                    Object result = JSONValue.parse(data);
                    JSONObject jsonObj = (JSONObject) result;
                    String Type = (String) jsonObj.get("Type");

                    String Response = "";
                    switch (Type) {
                        case "ThreeWordsCount":
                            String FirstWord = (String) jsonObj.get("FirstWord");
                            String SecondWord = (String) jsonObj.get("SecondWord");
                            String ThirdWord = (String) jsonObj.get("ThirdWord");
                            Response = CountThreeWords(FirstWord, SecondWord, ThirdWord);
                            break;

                        case "MultiThreadedWordsCount":
                            String outputFilePath = FileSystems.getDefault().getPath("data-output.txt").toString();
                            int nbThreads =Integer.parseInt(String.valueOf((Long) jsonObj.get("nbThreads")));
                            CountMultithreadedWord(filePath, outputFilePath,  nbThreads);
                            Response = "Please find the output in data-output.txt";
                            break;

                        case "AddWord":
                            String WordToAdd = (String) jsonObj.get("WordToAdd");
                            AddWord(WordToAdd);
                            Response = "Word '"+WordToAdd+"' added";
                            break;

                        case "ReplaceWord":
                            String oldWord = (String) jsonObj.get("OldWord");
                            String newWord = (String) jsonObj.get("NewWord");
                            ChangeWord(oldWord, newWord, filePath);

                            Response = "All words are now replaced!";
                            break;

                        default:
                            System.out.println("Error getting data");
                            Response = "Error getting data";
                    }

                    byte[] buffer = new byte[1024];
                    buffer = Response.getBytes();
                    returnPacket = new DatagramPacket(buffer, buffer.length,dataPacket.getAddress(), dataPacket.getPort());

                    datasocket.send(returnPacket);
                    datasocket.close();
                    datasocket = new DatagramSocket(portNumber);

                } catch (SocketTimeoutException e) {
                    break;
                } catch (IOException e) {
                    System.err.println(e);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }

        } catch (SocketException se) {
            System.err.println(se);
        }
    }

    private void AddWord(String wordToAdd) {
        try {
            lock.acquire();
            BufferedWriter out = new BufferedWriter(new FileWriter(fileName, true));
            out.write(wordToAdd + "\n");
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("exception occurred" + e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        finally {
            lock.release();
        }
    }

    public String CountThreeWords(String firstTextField, String secondTextField, String thirdTextField) throws InterruptedException {
        try {
            mutex.acquire();
            numReaders++;
            if (numReaders == 1)
                lock.acquire();
            mutex.release();

            long start1 = System.nanoTime();
            String TotalResult = "";
            Thread thread1 = null, thread2 = null, thread3 = null;
            WordCountRunnable wc1 = new WordCountRunnable(firstTextField);
            WordCountRunnable wc2 = new WordCountRunnable(secondTextField);
            WordCountRunnable wc3 = new WordCountRunnable(thirdTextField);

            if (!firstTextField.isBlank()) {
                thread1 = new Thread(wc1);
                thread1.start();
            }
            if (!secondTextField.isBlank()) {
                thread2 = new Thread(wc2);
                thread2.start();
            }
            if (!thirdTextField.isBlank()) {
                thread3 = new Thread(wc3);
                thread3.start();
            }

            if (thread1 != null) {
                thread1.join();
                TotalResult += wc1.getResult();
            }
            if (thread2 != null) {
                thread2.join();
                TotalResult += wc2.getResult();
            }
            if (thread3 != null) {
                thread3.join();
                TotalResult += wc3.getResult();
            }
            long end1 = System.nanoTime();
            System.out.println("Elapsed Time in nano seconds: " + (end1 - start1));

            return TotalResult;

        } catch (Exception ex) {
            throw new RuntimeException(ex);
        } finally {
            mutex.acquire();
            numReaders--;
            if (numReaders == 0)
                lock.release();
            mutex.release();
        }
    }

    public void CountMultithreadedWord(String filePath, String outputFile, int nbThreads) throws InterruptedException {
        try {
            mutex.acquire();
            numReaders++;
            if (numReaders == 1)
                lock.acquire();
            mutex.release();

            MultithreadedWordCount multithreadedWordCount = new MultithreadedWordCount(filePath, outputFile, nbThreads);
            multithreadedWordCount.CountWords();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            mutex.acquire();
            numReaders--;
            if (numReaders == 0)
                lock.release();
            mutex.release();
        }
    }

    public void ChangeWord(String word1, String word2, String filePath) {
        try {
            lock.acquire();
            Path fileName = Path.of(filePath);
            String str = Files.readString(fileName);
            System.out.println("Old file:\n" + str + "\n\n\n");
            str = str.replaceAll(word1, word2);
            System.out.println("New file:\n" + str);
            BufferedWriter out = new BufferedWriter(new FileWriter(filePath, false));
            out.write(str);
            out.close();
        } catch (Exception e) {
            System.err.println(e);
        } finally {
            lock.release();
        }
    }
}