package hagimule.client.downloader;

import java.io.*;
import java.net.*;
import java.util.List;

public class Downloader {

    private int totalFragments;

    public Downloader(int totalFragments) {
        this.totalFragments = totalFragments;
    }

    public void downloadFile(String fileName, List<String> daemonAddresses, long fileSize, String tempFolder) {
        long fragmentSize = fileSize / totalFragments; // Calcul de la taille de chaque fragment
        int part = 0;

        for (String daemonAddress : daemonAddresses) {
            long startByte = part * fragmentSize;
            long endByte = (part + 1) * fragmentSize;

            // Dernier fragment peut être plus grand
            if (part == totalFragments - 1) {
                endByte = fileSize;
            }

            fetchFragment(daemonAddress, fileName, startByte, endByte, tempFolder);
            part++;
        }
    }

    private void fetchFragment(String daemonAddress, String fileName, long startByte, long endByte, String tempFolder) {
        String[] parts = daemonAddress.split(":");
        String host = parts[0];
        int port = Integer.parseInt(parts[1]);

        try (Socket socket = new Socket(host, port);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             InputStream in = socket.getInputStream();
             FileOutputStream fos = new FileOutputStream(new File(tempFolder, fileName + ".part" + (startByte / (endByte - startByte))))) {

            // Envoyer la commande GET pour le fragment
            out.println("GET " + fileName + " " + startByte + " " + endByte);

            // Lire les données du fragment et les écrire dans le fichier temporaire
            byte[] buffer = new byte[8192];  // Taille du buffer pour la lecture
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
