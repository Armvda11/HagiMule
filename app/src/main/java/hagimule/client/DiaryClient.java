package hagimule.client;

import hagimule.diary.Diary;

import java.io.*;
import java.net.Socket;
import java.rmi.Naming;
import java.util.List;

public class DiaryClient {

    public static void main(String[] args) {
        try {
            // Connecter au Diary
            Diary diary = (Diary) Naming.lookup("rmi://localhost/Diary");

            // Nom du fichier à télécharger
            String fileName = "file1.txt";  // Le nom du fichier que vous voulez télécharger
            System.out.println("Demande du fichier : " + fileName);

            // Obtenir les adresses des Daemons qui possèdent le fichier
            List<String> daemonAddresses = diary.findDaemonAddressesByFile(fileName);
            if (daemonAddresses.isEmpty()) {
                System.out.println("Aucun Daemon ne possède ce fichier.");
                return;
            }

            System.out.println("Adresses des Daemons trouvées : " + daemonAddresses);

            // Choisir un Daemon pour récupérer le fichier (ici, on choisit le premier Daemon)
            String daemonAddress = daemonAddresses.get(0);

            // Télécharger le fichier complet à partir du Daemon
            downloadFile(daemonAddress, fileName);

            System.out.println("Fichier téléchargé et sauvegardé : " + fileName);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Télécharger un fichier complet depuis un Daemon.
     *
     * @param daemonAddress l'adresse du Daemon
     * @param fileName      le nom du fichier à télécharger
     */
    private static void downloadFile(String daemonAddress, String fileName) {
        String[] parts = daemonAddress.split(":");
        String host = parts[0];
        int port = Integer.parseInt(parts[1]);

        try (Socket socket = new Socket(host, port);
             InputStream in = socket.getInputStream();
             FileOutputStream fos = new FileOutputStream(fileName)) {

            // Envoyer la commande pour récupérer le fichier
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            out.println("GET " + fileName + " 0 " + Long.MAX_VALUE);  // Commande envoyée au Daemon

            // Lire le fichier du Daemon et l'écrire dans un fichier local
            byte[] buffer = new byte[8192];  // Buffer pour la lecture
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);  // Écrire les données dans le fichier
            }
            System.out.println("Fichier " + fileName + " reçu avec succès !");
        } catch (IOException e) {
            System.err.println("Erreur de téléchargement du fichier : " + e.getMessage());
            e.printStackTrace();
        }
    }
}
