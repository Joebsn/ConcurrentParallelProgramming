import org.json.simple.JSONObject;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
public class Client {
    private final int portNumber;
    private final JFrame frame;
    private JTextField firstWordField;
    private JTextField secondWordField;
    private JTextField thirdWordField;
    private JTextField wordToAddField;
    private JTextField wordToReplaceField;
    private JTextField alternativeWordField;
    private JTextField numberOfThreadsField;
    private JTextArea textArea;

    public Client(int portNumber) throws IOException {
        this.portNumber = portNumber;
        frame = new JFrame("WordsCounter");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        makeLabelsPanel();
        frame.add(makeTextAreaPanel(), BorderLayout.CENTER);
        frame.setSize(700, 800);
        frame.setVisible(true);
    }

    private void makeLabelsPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(20, 2));
        firstWordField = new JTextField(10);
        secondWordField = new JTextField(10);
        thirdWordField = new JTextField(10);
        numberOfThreadsField = new JTextField(10);
        wordToAddField = new JTextField(10);
        wordToReplaceField = new JTextField(10);
        alternativeWordField = new JTextField(10);

        panel.add(new JLabel("First Word"));
        panel.add(firstWordField);
        panel.add(new JLabel("Second Word"));
        panel.add(secondWordField);
        panel.add(new JLabel("Third Word"));
        panel.add(thirdWordField);
        JButton threeWordsThreadCountButton = new JButton("Search the Words");
        threeWordsThreadCountButton.addActionListener(new StartListener());
        JButton clearButton = new JButton("Clear");
        clearButton.addActionListener(new ClearListener());
        panel.add(threeWordsThreadCountButton);
        panel.add(clearButton);

        addEmptyLabels(panel, 2);

        panel.add(new JLabel("Number of threads"));
        panel.add(numberOfThreadsField);
        JButton multithreadingWordCountButton = new JButton("Multi-Threaded Word Count");
        multithreadingWordCountButton.addActionListener(new MultithreadedListener());
        panel.add(multithreadingWordCountButton);

        addEmptyLabels(panel, 3);

        panel.add(new JLabel("Word to Add"));
        panel.add(wordToAddField);
        JButton addWordButton = new JButton("Add word to file");
        addWordButton.addActionListener(new AddWordListener());
        panel.add(addWordButton);

        addEmptyLabels(panel, 3);

        panel.add(new JLabel("Old Word"));
        panel.add(wordToReplaceField);
        panel.add(new JLabel("New Word"));
        panel.add(alternativeWordField);
        JButton changeWordButton = new JButton("Replace Word in file");
        changeWordButton.addActionListener(new changeWordListener());
        panel.add(changeWordButton);
        panel.add(new JLabel(" "));panel.add(new JLabel(" "));

        frame.add(panel, BorderLayout.NORTH);
    }

    private void addEmptyLabels(JPanel panel, int numberOfLabels) {
        for(int i = 0; i < numberOfLabels; i++) {
            panel.add(new JLabel(" "));
        }
    }

    private JScrollPane makeTextAreaPanel() {
        JPanel panel = new JPanel();
        textArea = new JTextArea();
        panel.setLayout(new BorderLayout());
        panel.add(textArea);
        return new JScrollPane(panel);
    }

    private class StartListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            try {
                if(firstWordField.getText().isBlank() && secondWordField.getText().isBlank()&& thirdWordField.getText().isBlank()) {
                    JOptionPane.showMessageDialog(frame, "Please add at least one word!",
                            "Validation Error", JOptionPane.ERROR_MESSAGE);
                }
                else {
                    String RequestType = "ThreeWordsCount";
                    JSONObject request = new JSONObject();
                    request.put("Type",RequestType);
                    request.put("FirstWord", firstWordField.getText());
                    request.put("SecondWord", secondWordField.getText());
                    request.put("ThirdWord", thirdWordField.getText());

                    sendPacket(request);
                }
            }
            catch(Exception ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    private class ClearListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent arg0) {
            firstWordField.setText("");
            secondWordField.setText("");
            thirdWordField.setText("");
        }
    }

    private class MultithreadedListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent arg0) {
            try {
                if(numberOfThreadsField.getText().isBlank()) {
                    JOptionPane.showMessageDialog(frame, "Please fill the number of threads!",
                            "Validation Error", JOptionPane.ERROR_MESSAGE);
                }
                else {
                    int nbThreads = Integer.parseInt(numberOfThreadsField.getText().trim());

                    JSONObject request = new JSONObject();
                    request.put("Type", "MultiThreadedWordsCount");
                    request.put("nbThreads", nbThreads);

                    sendPacket(request);
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private class AddWordListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent arg0) {
            try {
                if(wordToAddField.getText().isBlank()) {
                    JOptionPane.showMessageDialog(frame, "Please add at least one word!",
                            "Validation Error", JOptionPane.ERROR_MESSAGE);
                }
                else {
                    JSONObject request = new JSONObject();
                    request.put("Type", "AddWord");
                    request.put("WordToAdd", wordToAddField.getText());

                    sendPacket(request);
                }
            }
            catch (Exception e)
            {
                System.err.println(e.getMessage());
            }
        }
    }

    private class changeWordListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent arg0) {
            try {
                if(wordToReplaceField.getText().isBlank() || alternativeWordField.getText().isBlank()){
                    JOptionPane.showMessageDialog(frame, "Please fill both fields!",
                            "Validation Error", JOptionPane.ERROR_MESSAGE);
                }

                else {
                    String oldWord = wordToReplaceField.getText().trim();
                    String newWord = alternativeWordField.getText().trim();

                    JSONObject request = new JSONObject();
                    request.put("Type","ReplaceWord");
                    request.put("OldWord",oldWord);
                    request.put("NewWord",newWord);

                    sendPacket(request);
                }
            } catch (SocketException | UnknownHostException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void sendPacket(JSONObject request) throws UnknownHostException, SocketException {
        DatagramPacket sPacket, rPacket;
        InetAddress ia = InetAddress.getByName("localhost");
        DatagramSocket dataSocket = new DatagramSocket();
        System.out.println("Starting Client Connection");
        try {
            byte[] buffer;
            buffer = request.toString().getBytes();
            sPacket = new DatagramPacket(buffer, buffer.length, ia, portNumber);
            dataSocket.send(sPacket);

            buffer = new byte[1024];
            rPacket = new DatagramPacket(buffer, buffer.length);
            dataSocket.receive(rPacket);

            String retString = new String((rPacket.getData()));
            textArea.append("\n" + retString);
            dataSocket.close();
            System.out.println("Stopping Client Connection");
        }
        catch (IOException e) {
            System.err.println(e);
        }
    }
}