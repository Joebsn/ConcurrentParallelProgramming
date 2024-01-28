import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class StartServer {

    public static void main(String[] args) throws IOException {
        Path outputFilePath = FileSystems.getDefault().getPath("target/data.txt");
        Files.copy(Paths.get("src/main/resources/data.txt"), outputFilePath, StandardCopyOption.REPLACE_EXISTING);
        Server server = new Server(outputFilePath.toString(), 4000);
        server.startServer();
    }
}