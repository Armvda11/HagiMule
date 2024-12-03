package hagimule.diary;

import java.rmi.Naming;
import java.util.List;

public class DiaryClient {
    public static void main(String[] args) {
        try {
            // Look up the remote Diary service from the RMI registry
            Diary diary = (Diary) Naming.lookup("rmi://localhost/Diary");

            // Register a file for a client
            diary.registerClientFile("Client1", "file1.txt");

            // Retrieve the files for a client
            List<String> files = diary.getClientFiles("Client1");
            System.out.println("Client1's files: " + files);

            // Remove a file for the client
            String removedFile = diary.removeClientFile("Client1", "file1.txt");
            System.out.println("Removed file: " + removedFile);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
