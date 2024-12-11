package hagimule.client.daemon;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class Daemon {
    
    private int port;// le port de communication
    private File file; // le fichier à telecharger

    /**
     * Daemon constructor
     * @param port communication port
     */
    public Daemon(int port) {
        this.port = port;
    }

    /**
     * Add a file to the daemon, associating it with his name ( for the client to request it)
     * @param fileName the name of the file
     * @param file   the file to add
     */
    public void addFile(String fileName, File file) {
        this.file = file;

        // URL de connexion JDBC (adaptée à votre base de données)
        String url = "jdbc:sqlite:" + System.getProperty("user.dir") + "\\src\\main\\java\\hagimule\\client\\daemon\\" + "testdb.db";
        String path = "C:/Users/Utilisateur/Documents/"; // Exemple de chemin d'accès au fichier
        String size = "2"; // Exemple de taille du fichier

        // Requête SQL pour insérer les données
        String sql = "INSERT INTO files (file_name, file_path, file_size) VALUES (" + fileName + "," + path +"," + size + ")";


        try (Connection connection = DriverManager.getConnection(url)) {
            System.out.println("Connexion réussie à H2 ! \n database créée ici : " + url);

            // Créer la table "files" si elle n'existe pas déjà
            String createTableSQL = "CREATE TABLE IF NOT EXISTS files (" +
                                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                                    "file_name VARCHAR(255), " +
                                    "file_path VARCHAR(255), " +
                                    "file_size INT)";
            try (PreparedStatement createStmt = connection.prepareStatement(createTableSQL)) {
                createStmt.executeUpdate();
            } catch (SQLException e) {
                System.err.println("Erreur SQL : " + e.getMessage());
            }

            // Exécuter l'insertion dans la base de données avec les valeurs par défaut
            String insertSQL = "INSERT INTO files (file_name, file_path, file_size) VALUES (?, ?, ?)";
            try (PreparedStatement insertStmt = connection.prepareStatement(insertSQL)) {
                insertStmt.setString(1, fileName);  // Nom du fichier
                insertStmt.setString(2, path);      // Chemin du fichier
                insertStmt.setString(3, size);      // Taille par défaut
                insertStmt.executeUpdate();
                System.out.println("Les informations ont été ajoutées avec succès."); 
                                  
            } catch (SQLException e) {
                System.err.println("Erreur SQL : " + e.getMessage());
            }
        } catch (SQLException e) {
            System.err.println("Erreur SQL : " + e.getMessage());
        }
    }

    /**
     * Start the daemon
     */
    public void start() {
        try (
            /** Suite
             * new ServerSocket(port, 50, InetAddress.getByName("0.0.0.0")); // to listen on all network interfaces
             */


            // Create a server socket, and listen for incoming connections
            ServerSocket serverSocket = new ServerSocket(port)) {
           
            System.out.println("Daemon à l'écoute sur le port " + port);
            // Accept incoming connections and maintain them
            while (true) {
                // Accept a new client connection
                try (
                    Socket clientSocket = serverSocket.accept()) {
                    
                    handleClientRequest(clientSocket);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Handle the client request, and send the requested file fragment
     * @param clientSocket the client socket
     * @throws IOException if an error occurs during the communication
     */
    private void handleClientRequest(Socket clientSocket) throws IOException {
        try (
            // Create the input and output streams , to read the client request and send the file fragment
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream())); // to read the client request
            OutputStream out = clientSocket.getOutputStream()) {
    
            // Read the client request
            String request = in.readLine(); // Read the client request
            if (request == null || request.isEmpty()) {
                out.write("Request invalide\n".getBytes());
                return;
            }
    
            String[] parts = request.split(" "); // Split the request into parts
            if (parts.length < 2) {  // Check if the request is valid
                out.write("Request invalide\n".getBytes());
                return;
            }
    
            String command = parts[0];
            String fileName = parts[1];
            
            // we chose to have only two commands : SIZE and GET
            // SIZE : to get the size of the file (the receiver will know how many fragments to expect)
            // GET : to get a fragment of the file
            switch (command) {
                case "SIZE":  
                    if (file != null && file.getName().equals(fileName)) {
                        out.write((file.length() + "\n").getBytes());
                    } else {
                        out.write("File not found\n".getBytes());
                    }
                    break;
    
                case "GET":
                    if (parts.length != 4) {
                        out.write("GET request invalid\n".getBytes());
                        return;
                    }
                    long startByte = Long.parseLong(parts[2]);
                    long endByte = Long.parseLong(parts[3]);
                    sendFileFragment(out, startByte, endByte);
                    break;
    
                default:
                    out.write("Unknown Command\n".getBytes());
            }
        }
    }
    
    /**
     * Send a fragment of the file to the client (from startByte to endByte) ,
     * @param out the output stream to send the fragment
     * @param startByte the start byte of the fragment
     * @param endByte the end byte of the fragment
     * @throws IOException if an error occurs during the communication
     */
    private void sendFileFragment(OutputStream out, long startByte, long endByte) throws IOException {
        try (
            // it's the way that i found to position the cursor at the startByte
            RandomAccessFile partFileToSend = new RandomAccessFile(file, "r")) {
            partFileToSend.seek(startByte);  // position the cursor at the startByte
            long remainingBytes = endByte - startByte; // calculate the remaining bytes to send
            byte[] buffer = new byte[8192];
            int bytesRead; 

            
            while (remainingBytes > 0 && (bytesRead = partFileToSend.read(buffer, 0, (int) Math.min(buffer.length, remainingBytes))) != -1) {
                out.write(buffer, 0, bytesRead);
                remainingBytes -= bytesRead;
            }
            out.flush(); // to make sure that the fragment is sent
            System.out.println("Fragment sent with success!");
        }
    }
    

}
