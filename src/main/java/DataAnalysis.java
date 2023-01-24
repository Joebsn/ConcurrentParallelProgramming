import org.json.simple.JSONObject;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.*;
import java.nio.file.FileSystems;

public class DataAnalysis
{
    private JFrame frame;
    JTabbedPane tabs;
    private JTextField firstTextField;
    private JTextField secondTextField;
    private JTextField thirdTextField;
    private JTextField stringToSendTextField;
    private JTextField replacedWord;
    private JTextField alternativeWord;
    private JTextField numberOfThread;
    private JTextArea textArea;
    private JButton ThreeWordsThreadCountButton;
    private JButton clearButton;
    private JButton mutlithreadedWordCountButton;
    private JButton AddWordButton;
    private JButton changeWordButton;
    private int portNumber = 4000, len = 1024;
    private final String filePath = FileSystems.getDefault().getPath("data.txt").toString();
    private final String outputFile = FileSystems.getDefault().getPath("Output.txt").toString();
    private Socket socket            = null;
    private DataInputStream  input   = null;
    private DataOutputStream out     = null;

    public DataAnalysis() throws IOException
    {
        frame = new JFrame("WordsCounter");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        makeLabelsPanel();
        frame.add(makeTextAreaPanel(), BorderLayout.CENTER);
        frame.setSize(700, 800);
        frame.setVisible(true);
    }

    private void makeLabelsPanel()
    {
        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(20, 2));
        firstTextField = new JTextField(10);
        secondTextField = new JTextField(10);
        thirdTextField = new JTextField(10);
        stringToSendTextField = new JTextField(10);
        replacedWord = new JTextField(10);
        alternativeWord = new JTextField(10);
        numberOfThread = new JTextField(10);
        panel.add(new JLabel("First Word:"));
        panel.add(firstTextField);
        panel.add(new JLabel("Second Word:"));
        panel.add(secondTextField);
        panel.add(new JLabel("Third Word:"));
        panel.add(thirdTextField);
        ThreeWordsThreadCountButton = new JButton("Search the Words");
        clearButton = new JButton("Clear");
        panel.add(new JLabel(" "));
        panel.add(new JLabel(" "));
        panel.add(ThreeWordsThreadCountButton);
        panel.add(clearButton);
        ThreeWordsThreadCountButton.addActionListener(new StartListener());
        clearButton.addActionListener(new ClearListener());
        panel.add(new JLabel(" "));
        panel.add(new JLabel(" "));


        panel.add(new JLabel("Number of threads:"));
        panel.add(numberOfThread);
        mutlithreadedWordCountButton = new JButton("Mutli-Threaded Word Count");
        mutlithreadedWordCountButton.addActionListener(new MultithreadedListener());
        panel.add(mutlithreadedWordCountButton);
        panel.add(new JLabel(" "));
        panel.add(new JLabel(" "));
        panel.add(new JLabel(" "));


        panel.add(new JLabel("Text Data:"));
        panel.add(stringToSendTextField);
        AddWordButton = new JButton("Add word to file");
        AddWordButton.addActionListener(new AddWordListener());
        panel.add(AddWordButton);
        panel.add(new JLabel(" "));
        panel.add(new JLabel(" "));
        panel.add(new JLabel(" "));


        panel.add(new JLabel("Old Word:"));
        panel.add(replacedWord);
        panel.add(new JLabel("New Word:"));
        panel.add(alternativeWord);

        changeWordButton = new JButton("Replace Word in file");
        changeWordButton.addActionListener(new changeWordListener());
        panel.add(changeWordButton);
        panel.add(new JLabel(" "));panel.add(new JLabel(" "));

        frame.add(panel, BorderLayout.NORTH);
    }

    private JScrollPane makeTextAreaPanel()
    {
        JPanel panel = new JPanel();
        textArea = new JTextArea();
        panel.setLayout(new BorderLayout());
        panel.add(textArea);
        JScrollPane scrollPane = new JScrollPane(panel);
        return scrollPane;
    }
    public class StartListener implements ActionListener
    {
        @Override
        public void actionPerformed(ActionEvent e)
        {
            try
            {
                if(firstTextField.getText().isBlank() && secondTextField.getText().isBlank()&&thirdTextField.getText().isBlank()){
                    JOptionPane.showMessageDialog(frame, "Please add at least one word!",
                            "Validation Error", JOptionPane.ERROR_MESSAGE);
                }
                else
                {
                    String RequestType = "ThreeWordsCount";
                    JSONObject request = new JSONObject();
                    request.put("Type",RequestType);
                    request.put("FirstWord",firstTextField.getText());
                    request.put("SecondWord",secondTextField.getText());
                    request.put("ThirdWord",thirdTextField.getText());

                    SendPacket(request);
                }
            }
            catch(Exception ex)
            {
                throw new RuntimeException(ex);
            }
        }
    }

    public class ClearListener implements ActionListener
    {
        @Override
        public void actionPerformed(ActionEvent arg0)
        {
            firstTextField.setText("");
            secondTextField.setText("");
            thirdTextField.setText("");
        }
    }

    public class MultithreadedListener implements ActionListener
    {
        @Override
        public void actionPerformed(ActionEvent arg0)
        {
            try
            {
                if(numberOfThread.getText().isBlank())
                {
                    JOptionPane.showMessageDialog(frame, "Please fill the number of threads!",
                            "Validation Error", JOptionPane.ERROR_MESSAGE);
                }
                else{
                    int nbThreads = Integer.parseInt(numberOfThread.getText().trim());

                    String RequestType = "MultiThreadedWordsCount";
                    JSONObject request = new JSONObject();
                    request.put("Type",RequestType);
                    request.put("nbThreads",nbThreads);

                    SendPacket(request);
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
    }

    public class AddWordListener implements ActionListener
    {
        @Override
        public void actionPerformed(ActionEvent arg0)
        {

            try
            {
                if(stringToSendTextField.getText().isBlank()){
                    JOptionPane.showMessageDialog(frame, "Please add at least one word!",
                            "Validation Error", JOptionPane.ERROR_MESSAGE);
                }

                else{
                    String RequestType = "AddWord";
                    JSONObject request = new JSONObject();
                    request.put("Type",RequestType);
                    request.put("WordToAdd",stringToSendTextField.getText());

                    SendPacket(request);
                }
            }
            catch (Exception e)
            {
                System.err.println(e.getMessage());
            }
        }
    }

    public class changeWordListener implements ActionListener
    {
        @Override
        public void actionPerformed(ActionEvent arg0)
        {
            try
            {
                if(replacedWord.getText().isBlank() || alternativeWord.getText().isBlank()){
                    JOptionPane.showMessageDialog(frame, "Please fill both fields!",
                            "Validation Error", JOptionPane.ERROR_MESSAGE);
                }

                else{
                    String oldWord = replacedWord.getText().trim();
                    String newWord = alternativeWord.getText().trim();

                    String RequestType = "ReplaceWord";
                    JSONObject request = new JSONObject();
                    request.put("Type",RequestType);
                    request.put("OldWord",oldWord);
                    request.put("NewWord",newWord);

                    SendPacket(request);
                }
            }
            catch (Exception e)
            {
                System.err.println(e);
            }
        }
    }

    public void SendPacket(JSONObject request) throws UnknownHostException, SocketException {
        String hostname = "localhost";
        DatagramPacket sPacket, rPacket;
        InetAddress ia = InetAddress.getByName(hostname);
        DatagramSocket datasocket = new DatagramSocket();
        System.out.println("Starting Client Connection");
        try
        {
            byte[] buffer;
            buffer = request.toString().getBytes();
            sPacket = new DatagramPacket(buffer, buffer.length, ia, portNumber);
            //sending the packet
            datasocket.send(sPacket);

            byte [] rbuffer = new byte[1024];
            rPacket = new DatagramPacket(rbuffer, rbuffer.length);
            datasocket.receive(rPacket);

            String retString = new String((rPacket.getData()));
            textArea.append("\n"+retString);
            datasocket.close();
            System.out.println("Stopping Client Connection");
        }
        catch (IOException eee)
        {
            System.err.println(eee);
        }
    }

    public static void main(String[] args) throws IOException
    {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    new DataAnalysis();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }
}