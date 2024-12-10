package hagimule.client;

import hagimule.diary.Diary;

import java.io.*;
import java.net.Socket;
import java.rmi.Naming;
import java.util.List;

public class DiaryClient {
    // optimisation idea
    // 1. use socker send instead of printwriter (and use byte[] instead of string)


    public static void main(String[] args) {
        try {
            /**
             * Suite:
             * nouvelle adresse pour le client : InetAddress.getLocalHost().getHostAddress()
             * fait < "rmi://cette nouvelle addres /Diary" > pour se connecter au Diary
             */


            // Connecter au Diary
            Diary diary = (Diary) Naming.lookup("rmi://localhost/Diary");

            String fileName = "file1.txt";
            System.out.println("Demande du fichier : " + fileName);

            // Obtenir les adresses des Daemons possédant le fichier
            List<String> daemonAddresses = diary.findDaemonAddressesByFile(fileName);
            if (daemonAddresses.isEmpty()) {
                System.out.println("Aucun Daemon ne possède ce fichier.");
                return;
            }

            System.out.println("Adresses des Daemons trouvées : " + daemonAddresses);

            // Obtenir la taille du fichier auprès d'un des Daemons
            long fileSize = getFileSize(daemonAddresses.get(0), fileName);
            if (fileSize <= 0) {
                System.out.println("Impossible d'obtenir la taille du fichier.");
                return;
            }

            System.out.println("Taille du fichier : " + fileSize + " octets");

            // Diviser le fichier en fragments dynamiquement
            String outputFolder = "";
            File outputFile = new File(outputFolder, "received_" + fileName);

            downloadFragments(fileName, daemonAddresses, fileSize, outputFolder);

            // Reconstituer le fichier
            reassembleFile(fileName, outputFolder, outputFile.getAbsolutePath());

            System.out.println("Fichier reconstitué avec succès : " + outputFile.getAbsolutePath());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * GetFilesize : method to get the size of the file
     * @param daemonAddress the address of the daemon
     * @param fileName      the name of the file
     * @return              the size of the file
     */
    private static long getFileSize(String daemonAddress, String fileName) {
        String[] parts = daemonAddress.split(":");
        String host = parts[0];
        int port = Integer.parseInt(parts[1]);
    
        try (
            // Create a socket to connect to the daemon, at the specified address and port
            Socket socket = new Socket(host, port);
            // !!!! j'ai utiliser un PrintWriter pour envoyer la commande SIZE car je l'es trouver bien mieux ( pas besoin de le convertir en byte)
            // !!! demandé à Freaky ????
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
    
            // send the command SIZE to get the size of the file
            out.println("SIZE " + fileName);
    
            // read the response from the daemon
            String response = in.readLine();
            try {
                return Long.parseUnsignedLong(response.trim()); // Convert the response to a long (the size of the file)
            } catch (NumberFormatException e) {
                System.err.println("Erreur lors de la récupération de la taille : " + response);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return -1; // En cas d'erreur
    }
    
    /**
     * DownloadFragments : method to download the fragments of the file
     * @param fileName          the name of the file
     * @param daemonAddresses   the addresses of the daemons
     * @param fileSize          the size of the file
     * @param outputFolder      the folder where to save the fragments
     * @throws IOException      if an error occurs during the download
     */
    private static void downloadFragments(String fileName, List<String> daemonAddresses, long fileSize,
                                          String outputFolder) throws IOException {

        long fragmentSize = fileSize / daemonAddresses.size();

        for (int i = 0; i < daemonAddresses.size(); i++) {
            String daemonAddress = daemonAddresses.get(i);
            long startByte = i * fragmentSize;
            long endByte = (i == daemonAddresses.size() - 1) ? fileSize : (startByte + fragmentSize);

            String fragmentPath = outputFolder + "/" + fileName + ".part" + i;
            downloadFragment(daemonAddress, fileName, startByte, endByte, fragmentPath);
        }
    }

    /**
     * DownloadFragment : method to download a fragment of the file
     * @param daemonAddress the address of the daemon
     * @param fileName      the name of the file
     * @param startByte     the strating byte of the fragment
     * @param endByte       the ending byte of the fragment
     * @param fragmentPath  the path where to save the fragment
     * @throws IOException  if an error occurs during the download
     */
    private static void downloadFragment(String daemonAddress, String fileName, long startByte, long endByte,
                                         String fragmentPath) throws IOException {
        String[] parts = daemonAddress.split(":");
        String host = parts[0];
        int port = Integer.parseInt(parts[1]);

        try (
            Socket socket = new Socket(host, port);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            InputStream in = socket.getInputStream();
            FileOutputStream fos = new FileOutputStream(fragmentPath)) {

            // send the command GET to get the fragment of the file
            out.println("GET " + fileName + " " + startByte + " " + endByte);

            // Lire et sauvegarder le fragment
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
            }
        }
    }

    private static void reassembleFile(String fileName, String tempFolder, String outputFilePath) throws IOException {
        try (
            FileOutputStream fos = new FileOutputStream(outputFilePath)) {
            int part = 0;
            while (true) {
                File fragment = new File(tempFolder, fileName + ".part" + part);
                if (!fragment.exists()) break;

                try (FileInputStream fis = new FileInputStream(fragment)) {
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = fis.read(buffer)) != -1) {
                        fos.write(buffer, 0, bytesRead);
                    }
                }
                fragment.delete();
                part++;
            }
        }
    }
}
