package hagimule.diary;

import java.rmi.Naming;
import java.util.List;

public class DiaryClient {
    public static void main(String[] args) {
        try {
            // Recherche le service Diary dans le registre RMI
            Diary diary = (Diary) Naming.lookup("rmi://localhost/Diary");

            // Enregistrer les fichiers "test1.txt", "Freaky", "wilkens"
            String clientName = "Client1";
            diary.registerClientFile(clientName, "test1.txt");
            diary.registerClientFile(clientName, "Freaky");
            diary.registerClientFile(clientName, "wilkens");

            // Afficher les fichiers enregistrés pour le client
            List<String> files = diary.getClientFiles(clientName);
            System.out.println("Files for client " + clientName + ": " + files);

            // Supprimer le fichier "test1.txt"
            String removedFile = diary.removeClientFile(clientName, "test1.txt");
            System.out.println("Removed file: " + removedFile);

            // Essayer de supprimer un fichier qui n'existe pas ("madame")
            try {
                removedFile = diary.removeClientFile(clientName, "madame");
                System.out.println("Removed file: " + removedFile);
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }

            // Afficher les fichiers après tentative de suppression
            files = diary.getClientFiles(clientName);
            System.out.println("Files for client " + clientName + " after attempting to remove 'madame': " + files);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}