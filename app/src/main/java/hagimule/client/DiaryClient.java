package hagimule.client;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.net.Socket;
import java.rmi.Naming;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import hagimule.diary.Diary;

public class DiaryClient {

    private static final long MIN_FRAGMENT_SIZE = 512 * 1024; // Taille minimale d'un fragment (512 Ko)


    public static void main(String[] args) {
        try {

            String diaryAddress = (args.length > 0) ? args[0] : "localhost";

            // Se connecter au Diary via RMI
            //Diary diary = (Diary) Naming.lookup("rmi://localhost/Diary");
            //Diary diary = (Diary) Naming.lookup("rmi://147.127.133.14/Diary");
            Diary diary = (Diary) Naming.lookup("rmi://" + diaryAddress + "/Diary");
            
            String fileName = "hagi.txt"; 
            System.out.println("Demande de téléchargement du fichier : " + fileName);

            // Récupérer la liste des Daemons qui possèdent le fichier
            List<String> daemonAddresses = diary.findDaemonAddressesByFile(fileName);
            if (daemonAddresses.isEmpty()) {
                System.out.println("Aucun Daemon ne possède ce fichier.");
                return;
            }
            System.out.println(daemonAddresses);


            // Obtenir la taille du fichier à partir d'un des Daemons
            long fileSize = getFileSize(daemonAddresses.get(0), fileName);
            if (fileSize <= 0) {
                System.out.println("Impossible d'obtenir la taille du fichier.");
                return;
            }

            String outputFilePath = "received_" + fileName;

            // Télécharger les fragments et reconstituer le fichier
            downloadFragments(fileName, daemonAddresses, fileSize, outputFilePath);

            System.out.println("Fichier reconstitué avec succès : " + outputFilePath);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Télécharge les fragments du fichier et les écrit directement dans le fichier final.
     * 
     * @param fileName      Nom du fichier
     * @param daemonAddresses Adresses des Daemons
     * @param fileSize      Taille du fichier
     * @param outputFilePath Chemin du fichier final reconstitué
     */
    private static void downloadFragments(String fileName, List<String> daemonAddresses, long fileSize, String outputFilePath) throws IOException {
        int totalFragments = (int) Math.ceil((double) fileSize / MIN_FRAGMENT_SIZE);
        int nbThreads = Math.min(totalFragments, daemonAddresses.size() * 2);
        ExecutorService executor = Executors.newFixedThreadPool(nbThreads);
    
        try (RandomAccessFile raf = new RandomAccessFile(outputFilePath, "rw")) {
            raf.setLength(fileSize);
        }
    
        for (int i = 0; i < totalFragments; i++) {
            int fragmentIndex = i;
            long startByte = i * MIN_FRAGMENT_SIZE;
            long endByte = Math.min(startByte + MIN_FRAGMENT_SIZE, fileSize);
    
            executor.submit(() -> {
                try {
                    // Rotation round-robin des Daemons
                    int daemonIndex = fragmentIndex % daemonAddresses.size();
                    String daemonAddress = daemonAddresses.get(daemonIndex);
                    
                    // Téléchargement du fragment
                    byte[] data = downloadFragment(daemonAddress, fileName, startByte, endByte);
                    
                    // Écriture du fragment dans le fichier final
                    try (RandomAccessFile raf = new RandomAccessFile(outputFilePath, "rw")) {
                        raf.seek(startByte);
                        raf.write(data);
                    }
    
                    System.out.println("Fragment " + fragmentIndex + " téléchargé et écrit de " + startByte + " à " + endByte);
                } catch (IOException e) {
                    System.err.println("Erreur d'écriture du fragment " + fragmentIndex + ": " + e.getMessage());
                }
            });
        }
    
        executor.shutdown();
        try {
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {

            e.printStackTrace();
        }
    }
    
    /**
     * Télécharge un fragment du fichier à partir d'un Daemon.
     * 
     * @param daemonAddress Adresse du Daemon (ex: 127.0.0.1:8080)
     * @param fileName      Nom du fichier
     * @param startByte     Byte de début
     * @param endByte       Byte de fin
     * @return              Les octets du fragment
     */
    private static byte[] downloadFragment(String daemonAddress, String fileName, long startByte, long endByte) throws IOException {
        String[] parts = daemonAddress.split(":");
        String host = parts[0];
        int port = Integer.parseInt(parts[1]);

        try (Socket socket = new Socket(host, port);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             InputStream in = socket.getInputStream()) {

            out.println("GET " + fileName + " " + startByte + " " + endByte);
            
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                baos.write(buffer, 0, bytesRead);
            }
            return baos.toByteArray();
        }
    }

    /**
     * Récupère la taille du fichier auprès d'un Daemon.
     * 
     * @param daemonAddress Adresse du Daemon
     * @param fileName      Nom du fichier
     * @return              Taille du fichier
     */
    private static long getFileSize(String daemonAddress, String fileName) {
        String[] parts = daemonAddress.split(":");
        String host = parts[0];
        int port = Integer.parseInt(parts[1]);

        try (Socket socket = new Socket(host, port);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            out.println("SIZE " + fileName);
            String response = in.readLine();
            return Long.parseUnsignedLong(response.trim());
        } catch (IOException | NumberFormatException e) {
            e.printStackTrace();
        }
        return -1;
    }
}
