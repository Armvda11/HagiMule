package hagimule.client;

import hagimule.diary.Diary;
import hagimule.client.daemon.Daemon;

import java.io.*;
import java.rmi.Naming;

/**
 * Classe qui crée des fichiers de 2 Mo pour différents clients et les enregistre dans le Diary.
 */
public class ClientFileCreator {

    public static void main(String[] args) {
        try {
            // Connecter au Diary
            Diary diary = (Diary) Naming.lookup("rmi://localhost/Diary");

            // Chemin du dossier partagé
            File sharedDirectory = new File("src/main/java/hagimule/shared");
            if (!sharedDirectory.exists()) {
                sharedDirectory.mkdirs(); // Crée le dossier s'il n'existe pas
            }

            // Créer des fichiers de 2 Mo
            File file1 = createFile(sharedDirectory, "file1.txt", 2 * 1024 * 1024); // 2 Mo
            File file2 = createFile(sharedDirectory, "file2.txt", 2 * 1024 * 1024); // 2 Mo
          
            // Démarrer les Daemons
            Daemon daemon1 = new Daemon(8080);
            Daemon daemon2 = new Daemon(8081);
            Daemon daemon3 = new Daemon(8082);

            daemon1.addFile("file1.txt", file1);
            daemon2.addFile("file1.txt", file2);
            daemon3.addFile("file2.txt", file2);

            new Thread(daemon1::start).start();
            new Thread(daemon2::start).start();
            new Thread(daemon3::start).start();

            // Enregistrer les fichiers et Daemons dans le Diary
            diary.registerFile("file1.txt", "Client1", "127.0.0.1:8080");
            diary.registerFile("file1.txt", "Client2", "127.0.0.1:8081");
            diary.registerFile("file2.txt", "Client3", "127.0.0.1:8082");

            System.out.println("Fichiers de 2 Mo créés et enregistrés dans le Diary.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * createFile : method to create a file with a specific size
     * @param directory     the directory where the file will be created
     * @param fileName      the name of the file
     * @param size          the size of the file
     * @return              the created file
     * @throws IOException  if an error occurs during the file creation
     */
    private static File createFile(File directory, String fileName, long size) throws IOException {
        File file = new File(directory, fileName);
        
        try (
            //
            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file))) {
            StringBuilder content = new StringBuilder();
            
            // Créer un contenu d'environ 1 Ko à répéter pour obtenir la taille désirée
            content.append("Contenu simulé du fichier. ").append(System.currentTimeMillis()).append("\n");

            byte[] contentBytes = content.toString().getBytes(); // Convertir en bytes
            long bytesWritten = 0;

            // Écrire des données dans le fichier jusqu'à atteindre la taille demandée
            while (bytesWritten < size) {
                bos.write(contentBytes);
                bytesWritten += contentBytes.length;
            }
        }

        System.out.println("Fichier créé : " + file.getAbsolutePath() + " (taille : " + size / (1024 * 1024) + " Mo)");
        return file;
    }
}
