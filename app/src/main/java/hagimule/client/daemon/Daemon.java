package hagimule.client.daemon;

import java.io.*;
import java.net.*;

public class Daemon {

    private int port;
    private File file;

    public Daemon(int port) {
        this.port = port;
    }

    public void addFile(String fileName, File file) {
        this.file = file;
    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Daemon à l'écoute sur le port " + port);
            while (true) {
                try (Socket clientSocket = serverSocket.accept()) {
                    handleClientRequest(clientSocket);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleClientRequest(Socket clientSocket) throws IOException {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {

            // Lire la requête du client
            String request = in.readLine();
            if (request == null) {
                out.println("Erreur: Requête vide.");
                return;
            }

            String[] parts = request.split(" ");
            if (parts.length != 4) {
                out.println("Erreur: La commande est mal formée. Exemple correct : GET fileName startByte endByte");
                return;
            }

            String fileName = parts[1];
            long startByte;
            long endByte;

            try {
                startByte = Long.parseLong(parts[2]);
                endByte = Long.parseLong(parts[3]);
            } catch (NumberFormatException e) {
                out.println("Erreur: startByte et endByte doivent être des entiers valides.");
                return;
            }

            // Vérifier si le fichier demandé existe
            if (!fileName.equals(file.getName())) {
                out.println("Erreur: Fichier non trouvé.");
                return;
            }

            // Envoyer le fichier complet
            sendFile(out, file);
        }
    }

    private void sendFile(PrintWriter out, File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[8192];  // Taille du buffer pour la lecture
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                out.write(new String(buffer, 0, bytesRead));  // Envoyer les données du fichier
            }
            out.flush();
            System.out.println("Fichier envoyé avec succès !");
        }
    }

    public static void main(String[] args) {
        Daemon daemon = new Daemon(8080);  // Choix du port (ici 8080)
        daemon.addFile("file1.txt", new File("file1.txt"));  // Le fichier à envoyer
        daemon.start();  // Démarrer le Daemon
    }
}
