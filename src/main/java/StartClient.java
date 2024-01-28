import javax.swing.SwingUtilities;
import java.io.IOException;

public class StartClient {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                new Client(4000);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }
}