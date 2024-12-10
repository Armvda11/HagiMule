package hagimule.client.downloader;

import java.io.*;
import java.net.*;
import java.util.List;

public class Downloader {
    
    private int totalFragments;

    /**
     * Constructor
     * @param totalFragments
     */
    public Downloader(int totalFragments) {
        this.totalFragments = totalFragments;
    }

    /**
     * downloadFile : method to download a file from Daemons
     * @param fileName          file name
     * @param daemonAddresses   list of Daemons addresses
     * @param fileSize          file size
     * @param tempFolder        temporary folder where the fragments will be saved
     */
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

    /**
     * fetchFragment : method to fetch a fragment of a file from a Daemon
     * @param daemonAddress daemon address
     * @param fileName      file name
     * @param startByte     start byte of the fragment
     * @param endByte       end byte of the fragment
     * @param tempFolder    temporary folder where the fragment will be saved
     */
    private void fetchFragment(String daemonAddress, String fileName, long startByte, long endByte, String tempFolder) {
        String[] parts = daemonAddress.split(":");
        String host = parts[0];
        int port = Integer.parseInt(parts[1]);

        try (
            Socket socket = new Socket(host, port);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             InputStream in = socket.getInputStream();
             FileOutputStream fos = new FileOutputStream(new File(tempFolder, fileName + ".part" + (startByte / (endByte - startByte))))) {

            // Send the GET request to the Daemon
            out.println("GET " + fileName + " " + startByte + " " + endByte);

            // Read the fragment from the Daemon and save it to the temporary folder
            byte[] buffer = new byte[8192];  
            int bytesRead;
    
            while ((bytesRead = in.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
