package hagimule.diary;

import java.rmi.Naming;
import java.util.List;

/**
 * Main class to start the client
 * Here we look up the Diary service in the RMI registry and use it to register and remove files for a client
 */
public class DiaryClient {
    public static void main(String[] args) {
        try {
            // Look up the Diary service in the RMI registry
            Diary diary = (Diary) Naming.lookup("rmi://localhost/Diary");

            // the client1 registers 3 files, "test1.txt", "Freaky" and "wilkens"
            String clientName = "Client1";
            diary.registerClientFile(clientName, "test1.txt");
            diary.registerClientFile(clientName, "Freaky");
            diary.registerClientFile(clientName, "wilkens");

            // Print the files for the client
            List<String> files = diary.getClientFiles(clientName);
            System.out.println("Files for client " + clientName + ": " + files);

            // Remove the file "test1.txt" for the client
            String removedFile = diary.removeClientFile(clientName, "test1.txt");
            System.out.println("Removed file: " + removedFile);

            // try to remove the file "madame" for the client
            try {
                removedFile = diary.removeClientFile(clientName, "madame");
                System.out.println("Removed file: " + removedFile);
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }

            // print the files for the client, after attempting to remove "madame"
            files = diary.getClientFiles(clientName);
            System.out.println("Files for client " + clientName + " after attempting to remove 'madame': " + files);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}