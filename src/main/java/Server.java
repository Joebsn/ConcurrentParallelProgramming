import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Semaphore;

public class Server {
    private final String COUNT_OUTPUT = FileSystems.getDefault().getPath("target/count-output.txt").toString();
    private final int portNumber;
    private int numReaders = 0;
    private final String filePath;
    private final Semaphore mutex = new Semaphore(1);
    private final Semaphore lock = new Semaphore(1);

    public Server(String filePath, int portNumber) {
        this.filePath = filePath;
        this.portNumber = portNumber;
    }

    public void startServer() {
        DatagramPacket dataPacket, returnPacket;
        try {
            DatagramSocket dataSocket = new DatagramSocket(portNumber);
            System.out.println("Starting Server");
            while (true) {
                try {
                    byte[] buf = new byte[1024];
                    dataPacket = new DatagramPacket(buf, buf.length);
                    dataSocket.receive(dataPacket);
                    String data = new String(dataPacket.getData(), 0, dataPacket.getLength());

                    Object result = JSONValue.parse(data);
                    JSONObject jsonObj = (JSONObject) result;
                    String type = (String) jsonObj.get("Type");

                    String response;
                    switch (type) {
                        case "ThreeWordsCount" -> {
                            String firstWord = (String) jsonObj.get("FirstWord");
                            String secondWord = (String) jsonObj.get("SecondWord");
                            String thirdWord = (String) jsonObj.get("ThirdWord");
                            response = countThreeWords(firstWord, secondWord, thirdWord);
                        }
                        case "MultiThreadedWordsCount" -> {
                            int nbThreads = Integer.parseInt(String.valueOf(jsonObj.get("nbThreads")));
                            countMultithreadedWord(filePath, COUNT_OUTPUT, nbThreads);
                            response = "Please find the output in " + COUNT_OUTPUT;
                        }
                        case "AddWord" -> {
                            String wordToAdd = (String) jsonObj.get("WordToAdd");
                            addWord(wordToAdd);
                            response = "Word '" + wordToAdd + "' added";
                        }
                        case "ReplaceWord" -> {
                            String oldWord = (String) jsonObj.get("OldWord");
                            String newWord = (String) jsonObj.get("NewWord");
                            changeWord(oldWord, newWord);
                            response = oldWord + " replaced by " + newWord + " in " + filePath;
                        }
                        default -> {
                            System.out.println("Error getting data");
                            response = "Error getting data";
                        }
                    }

                    byte[] buffer = response.getBytes();
                    returnPacket = new DatagramPacket(buffer, buffer.length, dataPacket.getAddress(), dataPacket.getPort());

                    dataSocket.send(returnPacket);
                    dataSocket.close();
                    dataSocket = new DatagramSocket(portNumber);

                } catch (SocketTimeoutException e) {
                    break;
                } catch (IOException e) {
                    System.err.println(e);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }

        } catch (SocketException e) {
            System.err.println(e);
        }
    }

    private void addWord(String wordToAdd) {
        try {
            lock.acquire();
            BufferedWriter out = new BufferedWriter(new FileWriter(filePath, true));
            out.write("\n" + wordToAdd);
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("exception occurred" + e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            lock.release();
        }
    }

    private String countThreeWords(String firstTextField, String secondTextField, String thirdTextField) throws InterruptedException {
        try {
            mutex.acquire();
            numReaders++;
            if (numReaders == 1) {
                lock.acquire();
            }
            mutex.release();

            long start1 = System.nanoTime();
            String totalResult = "";
            Thread thread1 = null, thread2 = null, thread3 = null;
            WordCountRunnable wc1 = new WordCountRunnable(firstTextField, filePath);
            WordCountRunnable wc2 = new WordCountRunnable(secondTextField, filePath);
            WordCountRunnable wc3 = new WordCountRunnable(thirdTextField, filePath);

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
                totalResult += wc1.getResult();
            }
            if (thread2 != null) {
                thread2.join();
                totalResult += wc2.getResult();
            }
            if (thread3 != null) {
                thread3.join();
                totalResult += wc3.getResult();
            }
            long end1 = System.nanoTime();
            System.out.println("Elapsed Time in nano seconds: " + (end1 - start1));

            return totalResult;

        } catch (Exception ex) {
            throw new RuntimeException(ex);
        } finally {
            mutex.acquire();
            numReaders--;
            if (numReaders == 0) {
                lock.release();
            }
            mutex.release();
        }
    }

    private void countMultithreadedWord(String filePath, String outputFile, int nbThreads) throws InterruptedException {
        try {
            mutex.acquire();
            numReaders++;
            if (numReaders == 1) {
                lock.acquire();
            }
            mutex.release();

            MultithreadedWordCount multithreadedWordCount = new MultithreadedWordCount(filePath, outputFile, nbThreads);
            multithreadedWordCount.countWords();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            mutex.acquire();
            numReaders--;
            if (numReaders == 0) {
                lock.release();
            }
            mutex.release();
        }
    }

    private void changeWord(String word1, String word2) {
        try {
            lock.acquire();
            String str = Files.readString(Path.of(filePath));
            str = str.replaceAll("\\b" + word1 + "\\b", word2);
            BufferedWriter out = new BufferedWriter(new FileWriter(filePath, false));
            out.write(str);
            out.close();
        } catch (IOException | InterruptedException e) {
            System.err.println(e);
        } finally {
            lock.release();
        }
    }
}