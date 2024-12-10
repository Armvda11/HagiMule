package hagimule.client;

import hagimule.diary.Diary;
import hagimule.client.daemon.Daemon;

import java.io.*;
import java.net.InetAddress;
import java.rmi.Naming;
import java.util.UUID;

/**
 * Classe Client universelle
 * Chaque client crée un fichier et démarre un Daemon qui partage ce fichier
 */
public class ClientFileCreator {

    public static void main(String[] args) {
        try {
            // Lecture des paramètres d'exécution (nom du fichier, taille et port)
            String clientName = (args.length > 0) ? args[0] : "Client_" + UUID.randomUUID();
            String fileName = (args.length > 1) ? args[1] : "file" + clientName + ".txt";
            long fileSize = (args.length > 2) ? Long.parseLong(args[2]) : 2 * 1024 * 1024; // Par défaut 2 Mo
            int daemonPort = (args.length > 3) ? Integer.parseInt(args[3]) : 8080;

            System.out.println("Lancement de " + clientName);
            System.out.println("Fichier : " + fileName);
            System.out.println("Taille du fichier : " + (fileSize / (1024 * 1024)) + " Mo");
            System.out.println("Port du Daemon : " + daemonPort);

            // Connexion au Diary ( le diary 147.27.133.14   ( pixie ))
            Diary diary = (Diary) Naming.lookup("rmi://localhost/Diary");

            // Chemin du dossier partagé
            File sharedDirectory = new File("src/main/java/hagimule/shared");
            if (!sharedDirectory.exists()) {
                sharedDirectory.mkdirs(); // Crée le dossier s'il n'existe pas
            }

            // Crée un fichier unique de la taille demandée
            File file = createFile(sharedDirectory, fileName, fileSize);
            
            // Démarre le Daemon sur le port défini
            Daemon daemon = new Daemon(daemonPort);
            daemon.addFile(file.getName(), file);
            new Thread(daemon::start).start();

            // Enregistre le fichier et le Daemon dans le Diary
            String machineIP = InetAddress.getLocalHost().getHostAddress();
            String daemonAddress = machineIP + ":" + daemonPort;
            diary.registerFile(file.getName(), clientName, daemonAddress);

            System.out.println("Fichier '" + fileName + "' de " + (fileSize / (1024 * 1024)) + " Mo créé et enregistré dans le Diary.");
            System.out.println("Daemon à l'écoute sur : " + daemonAddress);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Méthode utilitaire pour créer un fichier de taille spécifiée.
     * 
     * @param directory Le dossier où créer le fichier.
     * @param fileName Le nom du fichier.
     * @param size La taille du fichier à créer (en octets).
     * @return Le fichier créé.
     * @throws IOException En cas d'erreur de création du fichier.
     */
    private static File createFile(File directory, String fileName, long size) throws IOException {
        File file = new File(directory, fileName);
        
        try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file))) {
            StringBuilder content = new StringBuilder();
            content.append("Contenu du fichier simulé. ").append(System.currentTimeMillis()).append("\n");
            byte[] contentBytes = content.toString().getBytes(); // Convertir en bytes
            long bytesWritten = 0;

            while (bytesWritten < size) {
                bos.write(contentBytes);
                bytesWritten += contentBytes.length;
            }
        }

        System.out.println("Fichier créé : " + file.getAbsolutePath() + " (taille : " + size / (1024 * 1024) + " Mo)");
        return file;
    }
}
