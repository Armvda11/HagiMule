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
import java.util.List;

import com.google.common.hash.Hashing;
import com.google.common.io.ByteSource;
import com.google.common.hash.HashCode;

public class Daemon {
    
    private final int port;// le port de communication
    private final String emplacement; // l'emplacement du daemon
    private File filePartage; // le fichier à telecharger

    // URL de connexion JDBC (adaptée à votre base de données)
    String urlDatabase;

    /**
     * Daemon constructor
     * @param port communication port
     */
    public Daemon(int port) {
        this.port = port;
        this.emplacement = System.getProperty("user.dir") + "\\src\\main\\java\\hagimule\\client\\daemon\\" ;
        this.urlDatabase = "jdbc:sqlite:" +  emplacement + "fichiers.db";
        creerDatabase();
    }

    public void addFile(String fileName, File file) {
        this.filePartage = file;
    }

    public String calculerChecksum(File file) {
        try {
            // Création d'une source de bytes à partir du fichier
            ByteSource byteSource = com.google.common.io.Files.asByteSource(file);
    
            // Calcul du hash SHA-256
            HashCode hashCode = byteSource.hash(Hashing.sha256());
    
            // Convertir le hash en chaîne de caractères
            return hashCode.toString();
        } catch (IOException e) {
            System.err.println("Erreur lors de la création du checksum : " + e.getMessage());
            e.printStackTrace();
            return ""; // Retourne une chaîne vide en cas d'erreur
        }
    }

    // Créer la base de données si elle n'existe pas lors de la création du daemon
    private void creerDatabase() {
        try (Connection connection = DriverManager.getConnection(urlDatabase)) {
            System.out.println("Connexion réussie à SQLlite ! \n database créée ici : " + urlDatabase);
             // Créer la table "files" si elle n'existe pas déjà
             String createTableSQL = "CREATE TABLE IF NOT EXISTS files (" +
             "id INT AUTO_INCREMENT PRIMARY KEY, " +
             "file_name VARCHAR(255), " +
             "file_path VARCHAR(255), " +
             "file_size INT, " +
             "checksum VARCHAR(255), " +
             "UNIQUE(checksum))";
            
            try (PreparedStatement createStmt = connection.prepareStatement(createTableSQL)) {
                createStmt.executeUpdate();
                System.out.println("Connexion réussie à SQLlite ! \n database créée ici : " + urlDatabase);
            } catch (SQLException e) {
                System.err.println("Erreur SQL : " + e.getMessage());
            }
        } catch (SQLException e) {
            System.err.println("Erreur SQL : " + e.getMessage());
        }
    }

    /**
     * Récupère la liste des noms des fichiers partagés dans la base de données
     * @return la liste des noms des fichiers partagés
     */    
    public List<String> getFilesNames() {
        List<String> filesNames = null;   
        
        // Récupérer la liste des noms des fichiers partagés dans la base de données
        // et les ajouter à la liste filesNames
        try (Connection connection = DriverManager.getConnection(urlDatabase)) {
            String selectSQL = "SELECT file_name FROM files";

            try (PreparedStatement selectStmt = connection.prepareStatement(selectSQL);
                java.sql.ResultSet resultSet = selectStmt.executeQuery()) {

                filesNames = new java.util.ArrayList<>();
                while (resultSet.next()) {
                    filesNames.add(resultSet.getString("file_name"));
                }

            } catch (SQLException e) {
            System.err.println("SQL error: " + e.getMessage());
            }
        } catch (SQLException e) {
            System.err.println("SQL error: " + e.getMessage());
        }
        return filesNames;
    }

    /**
     * Met à jour la base de données avec les fichiers partagés
        **/
    public void majDatabase() {
        String fileName;
        Long size;
        String insertSQL;

        String checksumFichier;

        // Chemin du dossier partagé
        String pathPartages = emplacement + "Fichiers"; // Exemple de chemin d'accès aux fichiers partagés

        File sharedDirectory = new File(pathPartages);
        if (!sharedDirectory.exists()) {
            sharedDirectory.mkdirs(); // Crée le dossier s'il n'existe pas
            System.err.println("Aucun dossier partagé trouvé.");
        }

        System.out.println("Début de l'insertion des fichiers");
        // Loop through all files in the shared directory and add them to the database
        for (File file : sharedDirectory.listFiles()) {
            if (file.isFile()) {
                fileName = file.getName();
                size = file.length();
                System.out.println("Insertion de : " + fileName);

                try (Connection connection = DriverManager.getConnection(urlDatabase)) {
                    checksumFichier = calculerChecksum(file);
                    insertSQL = "INSERT OR IGNORE INTO files (file_name, file_path, file_size, checksum) VALUES (?, ?, ?, ?)";

                    try (PreparedStatement insertStmt = connection.prepareStatement(insertSQL)) {
                        insertStmt.setString(1, fileName);
                        insertStmt.setString(2, file.getAbsolutePath());
                        insertStmt.setLong(3, size);
                        insertStmt.setString(4, checksumFichier);
                        insertStmt.executeUpdate();
                        System.out.println("File " + fileName + " added to the database.");
                    } catch (SQLException e) {
                    System.err.println("SQL error: " + e.getMessage());
                    }
                } catch (SQLException e) {
                    System.err.println("SQL error: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Charge le fichier à transférer dans le daemon
     * @param fileName the name of the file
     */
    public long setFile(String fileName) {
        long fileSize = -1;
        String selectSQL;
        String filePath;

        try (Connection connection = DriverManager.getConnection(urlDatabase)) {
            selectSQL = "SELECT file_path, file_size FROM files WHERE file_name = ?";
            try (PreparedStatement selectStmt = connection.prepareStatement(selectSQL)) {
                selectStmt.setString(1, fileName);
                selectStmt.execute();
                try (java.sql.ResultSet resultSet = selectStmt.getResultSet()) {
                    if (resultSet.next()) {
                        filePath = resultSet.getString("file_path");
                        fileSize = resultSet.getLong("file_size");
                        this.filePartage = new File(filePath);
                        System.out.println("File " + fileName + " loaded into the daemon.");
                    } else {
                        System.err.println("File not found in the database.");
                    }
                }
            } catch (SQLException e) {
                System.err.println("SQL error: " + e.getMessage());
            }
        } catch (SQLException e) {
            System.err.println("SQL error: " + e.getMessage());
        }
        return fileSize;
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
                case "SIZE" -> {  
                    if (filePartage != null && filePartage.getName().equals(fileName)) {
                        out.write((filePartage.length() + "\n").getBytes());
                    } else {
                        out.write("File not found\n".getBytes());
                    }
                }
    
                case "GET" -> {
                    if (parts.length != 4) {
                        out.write("GET request invalid\n".getBytes());
                        return;
                    }
                    long startByte = Long.parseLong(parts[2]);
                    long endByte = Long.parseLong(parts[3]);
                    sendFileFragment(out, startByte, endByte);
                }
    
                default -> out.write("Unknown Command\n".getBytes());
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
            RandomAccessFile partFileToSend = new RandomAccessFile(filePartage, "r")) {
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
