package hagimule.client.daemon;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.RandomAccessFile;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.ArrayList;

import com.google.common.hash.Hashing;
import com.google.common.io.ByteSource;

import hagimule.client.Compressor.FileCompressor;
import hagimule.client.Compressor.FileCompressorZstd;
import hagimule.client.Compressor.FileCompressorLZ4;
import hagimule.client.Compressor.FileCompressorLZMA;
import hagimule.client.Compressor.FileCompressorVide;

import com.google.common.hash.HashCode;

public class Daemon {
    
    private final int port; // communication port
    private File filePartage; // file to download
    private final FileCompressor compressor;
    // Path to the shared folder
    private String sharedFolder = System.getProperty("user.dir") + "/" + "Shared/"; // Example path to shared files
    private String emplacement = System.getProperty("user.dir") + "/";
    // JDBC connection URL (adapted to your database)
    private int latency = 0;
    String urlDatabase;

    private PrintStream logStream;
    private List<String> removedFiles = new ArrayList<>();

    /**
     * Daemon constructor
     * @param port communication port
     */
    public Daemon(int port) {
        this(port, System.getProperty("user.dir") + "/shared/", "zstd", 0);
    }

    public Daemon(int port, String sharedFolder) {
        this(port, sharedFolder, "zstd", 0);
    }

    public Daemon(int port, String sharedFolder, String compressorType, int latency) {
        this.port = port;
        this.sharedFolder = sharedFolder;
        this.compressor = createCompressor(compressorType);
        setupLogging();
        int lastIndex = sharedFolder.lastIndexOf("/");
        int secondLastIndex = sharedFolder.lastIndexOf("/", lastIndex - 1);
        this.emplacement = secondLastIndex > 0 ? sharedFolder.substring(0, secondLastIndex + 1) : "";
        this.urlDatabase = "jdbc:sqlite:" +  emplacement + "fichiers.db";
        File folder = new File(this.sharedFolder);
        if (!folder.exists()) {
            folder.mkdirs();
        }
        compressSharedFiles();
        createDatabase();
    }

    private FileCompressor createCompressor(String compressorType) {
        switch (compressorType.toLowerCase()) {
            case "lz4":
                return new FileCompressorLZ4();
            case "lzma":
                return new FileCompressorLZMA();
            case "vide":
                return new FileCompressorVide();
            case "zstd":
            default:
                return new FileCompressorZstd(22); // Default compressor
        }
    }

    private void setupLogging() {
        try {
            String logFilePath = System.getProperty("user.dir") + "/logs/logs_daemon_" + this.port + ".txt";
            logStream = new PrintStream(new FileOutputStream(logFilePath, true), true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void log(String message) {
        logStream.println(message);
    }

    private void logError(String message, Throwable t) {
        logStream.println(message);
        t.printStackTrace(logStream);
    }

    public void addFile(File file) {
        this.filePartage = file;
    }

    public static String calculateChecksum(File file) {
        try {
            ByteSource byteSource = com.google.common.io.Files.asByteSource(file);
            HashCode hashCode = byteSource.hash(Hashing.sha256());
            return hashCode.toString();
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }

    // Create the database if it does not exist when the daemon is created
    private void createDatabase() {
        try (Connection connection = DriverManager.getConnection(urlDatabase)) {
            log("Successfully connected to SQLite!");
            // Create the "files" table if it does not already exist
            String createTableSQL = "CREATE TABLE IF NOT EXISTS files (" +
            "id INT AUTO_INCREMENT PRIMARY KEY, " +
            "file_name VARCHAR(255), " +
            "file_path VARCHAR(255), " +
            "file_size INT, " +
            "checksum VARCHAR(255), " +
            "compressor_type VARCHAR(50), " + // Add compressor type field
            "UNIQUE(checksum))";
            
            try (PreparedStatement createStmt = connection.prepareStatement(createTableSQL)) {
                createStmt.executeUpdate();
                log("database created here: " + urlDatabase);
            } catch (SQLException e) {
                logError("SQL error: " + e.getMessage(), e);
            }
        } catch (SQLException e) {
            logError("SQL error: " + e.getMessage(), e);
        }
    }

    /**
     * Retrieves the list of shared file names in the database
     * @return the list of shared file names
     */
    public List<String> getFilesNames() {
        List<String> filesNames = null;
        
        // Retrieve the list of shared file names in the database
        // and add them to the filesNames list
        try (Connection connection = DriverManager.getConnection(urlDatabase)) {
            String selectSQL = "SELECT file_name FROM files";

            try (PreparedStatement selectStmt = connection.prepareStatement(selectSQL);
                java.sql.ResultSet resultSet = selectStmt.executeQuery()) {

                filesNames = new java.util.ArrayList<>();
                while (resultSet.next()) {
                    filesNames.add(resultSet.getString("file_name"));
                }

            } catch (SQLException e) {
                logError("SQL error: " + e.getMessage(), e);
            }
        } catch (SQLException e) {
            logError("SQL error: " + e.getMessage(), e);
        }
        return filesNames;
    }

    /**
     * Updates the database with shared files
     **/
    public void updateDatabase() {
        String fileName;
        Long size;
        String insertSQL;
        String fileChecksum;

        File sharedDirectory = new File(this.sharedFolder);
        if (!sharedDirectory.exists()) {
            sharedDirectory.mkdirs(); // Create the folder if it does not exist
            log("No shared folder found.");
        }

        log("Starting file insertion");
        // Loop through all files in the shared directory and add them to the database
        for (File file : sharedDirectory.listFiles()) {
            if (file.isFile()) {
                String name = file.getName();
                int dotIndex = name.lastIndexOf('.');
                if (dotIndex > 0) {
                    name = name.substring(0, dotIndex);
                }
                fileName = name;
                size = file.length();
                fileChecksum = calculateChecksum(file); // Calculate decompressed checksum
                log("Inserting: " + fileName);

                try (Connection connection = DriverManager.getConnection(urlDatabase)) {
                    insertSQL = "INSERT INTO files (file_name, file_path, file_size, checksum, compressor_type) VALUES (?, ?, ?, ?, ?) " +
                                "ON CONFLICT(checksum) DO UPDATE SET file_name = excluded.file_name, file_path = excluded.file_path";

                    try (PreparedStatement insertStmt = connection.prepareStatement(insertSQL)) {
                        insertStmt.setString(1, fileName);
                        insertStmt.setString(2, file.getAbsolutePath());
                        insertStmt.setLong(3, size);
                        insertStmt.setString(4, fileChecksum); // Set decompressed checksum
                        insertStmt.setString(5, this.compressor.getExtension().substring(1)); // Set compressor type without the dot
                        insertStmt.executeUpdate();
                        log("File " + fileName + " added to the database.");
                    } catch (SQLException e) {
                        logError("SQL error: " + e.getMessage(), e);
                    }
                } catch (SQLException e) {
                    logError("SQL error: " + e.getMessage(), e);
                }
            }
        }

        // Remove files from the database that are no longer in the shared directory
        try (Connection connection = DriverManager.getConnection(urlDatabase)) {
            String selectSQL = "SELECT file_name, file_path FROM files";
            try (PreparedStatement selectStmt = connection.prepareStatement(selectSQL);
                java.sql.ResultSet resultSet = selectStmt.executeQuery()) {

                while (resultSet.next()) {
                    fileName = resultSet.getString("file_name");
                    String filePath = resultSet.getString("file_path");
                    File file = new File(filePath);
                    if (!file.exists()) {
                        this.removedFiles.add(fileName);
                        String deleteSQL = "DELETE FROM files WHERE file_name = ?";
                        try (PreparedStatement deleteStmt = connection.prepareStatement(deleteSQL)) {
                            deleteStmt.setString(1, fileName);
                            deleteStmt.executeUpdate();
                            log("File " + fileName + " removed from the database.");
                        } catch (SQLException e) {
                            logError("SQL error: " + e.getMessage(), e);
                        }
                    }
                }
            } catch (SQLException e) {
                logError("SQL error: " + e.getMessage(), e);
            }
        } catch (SQLException e) {
            logError("SQL error: " + e.getMessage(), e);
        }
    }

    public List<String> getRemovedFiles() {
        List<String> filesToRemove = new ArrayList<>(this.removedFiles);
        this.removedFiles.clear();
        return filesToRemove;
    }

    public void compressSharedFiles() {
        if (this.sharedFolder == null) {
            logError("Shared folder is not set.", new Throwable());
            return;
        }
        
        File sharedDirectory = new File(this.sharedFolder);
        if (!sharedDirectory.exists() || !sharedDirectory.isDirectory()) {
            logError("Shared folder does not exist or is not a directory: " + this.sharedFolder, new Throwable());
            return;
        }
        for (File file : sharedDirectory.listFiles()) {
            // Skip if the file is already compressed in the database
            if (isFileCompressedInDatabase(file)) {
                continue;
            }
            String filename = file.getName();
            // Check if the file ends with any known compression extension
            if (filename.endsWith(".lz4") || filename.endsWith(".lzma") || filename.endsWith(".vide") || filename.endsWith(".zst")) {
                addFileToDatabase(file);
                continue;
            }
            // Skip if the file is already compressed based on the current compressor's extension
            if (filename.endsWith(this.compressor.getExtension())) {
                continue;
            }
            Path inputFile = Paths.get(file.getAbsolutePath());
            try {
                Path compressedFile = this.compressor.compressFile(inputFile);
                log("Fichier compressé : " + compressedFile);
                // Remove the original file after compression
                if (file.delete()) {
                    log("Original file deleted: " + file.getAbsolutePath());
                } else {
                    logError("Failed to delete original file: " + file.getAbsolutePath(), new Throwable());
                }
            } catch (IOException e) {
                logError("Error compressing file: " + e.getMessage(), e);
            }
        }
    }

    private void addFileToDatabase(File file) {
        String fileName = file.getName();
        Long size = file.length();
        String fileChecksum = calculateChecksum(file); // Calculate decompressed checksum
        String compressorType = getCompressorTypeFromExtension(fileName);

        try (Connection connection = DriverManager.getConnection(urlDatabase)) {
            String insertSQL = "INSERT INTO files (file_name, file_path, file_size, checksum, compressor_type) VALUES (?, ?, ?, ?, ?) " +
                               "ON CONFLICT(checksum) DO UPDATE SET file_name = excluded.file_name, file_path = excluded.file_path";

            try (PreparedStatement insertStmt = connection.prepareStatement(insertSQL)) {
                insertStmt.setString(1, fileName);
                insertStmt.setString(2, file.getAbsolutePath());
                insertStmt.setLong(3, size);
                insertStmt.setString(4, fileChecksum); // Set decompressed checksum
                insertStmt.setString(5, compressorType); // Set compressor type
                insertStmt.executeUpdate();
                log("File " + fileName + " added to the database.");
            } catch (SQLException e) {
                logError("SQL error: " + e.getMessage(), e);
            }
        } catch (SQLException e) {
            logError("SQL error: " + e.getMessage(), e);
        }
    }

    private String getCompressorTypeFromExtension(String fileName) {
        if (fileName.endsWith(".lz4")) {
            return "lz4";
        } else if (fileName.endsWith(".lzma")) {
            return "lzma";
        } else if (fileName.endsWith(".vide")) {
            return "vide";
        } else if (fileName.endsWith(".zstd")) {
            return "zstd";
        } else {
            return "";
        }
    }

    private boolean isFileCompressedInDatabase(File file) {
        String selectSQL = "SELECT compressor_type FROM files WHERE file_name = ?";
        try (Connection connection = DriverManager.getConnection(urlDatabase);
             PreparedStatement selectStmt = connection.prepareStatement(selectSQL)) {
            selectStmt.setString(1, file.getName());
            try (java.sql.ResultSet resultSet = selectStmt.executeQuery()) {
                if (resultSet.next()) {
                    String compressorType = resultSet.getString("compressor_type");
                    return compressorType != null && !compressorType.isEmpty();
                }
            }
        } catch (SQLException e) {
            logError("SQL error: " + e.getMessage(), e);
        }
        return false;
    }

    /**
     * Load the file to be transferred into the daemon
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
                        log("File " + fileName + " loaded into the daemon.");
                    } else {
                        log("File not found in the database.");
                    }
                }
            } catch (SQLException e) {
                logError("SQL error: " + e.getMessage(), e);
            }
        } catch (SQLException e) {
            logError("SQL error: " + e.getMessage(), e);
        }
        return fileSize;
    }

    /**
     * Start the daemon
     */
    public void start() {
        try (
            // Create a server socket, and listen for incoming connections
            ServerSocket serverSocket = new ServerSocket(port)) {

            log("Daemon listening on port " + port);
            // Accept incoming connections and maintain them
            while (true) {
                // Accept a new client connection
                try (
                    Socket clientSocket = serverSocket.accept()) {
                    
                    handleClientRequest(clientSocket);
                }
            }
        } catch (IOException e) {
            logError("IOException: " + e.getMessage(), e);
        }
    }

    /**
     * Handle the client request, and send the requested file fragment
     * @param clientSocket the client socket
     * @throws IOException if an error occurs during the communication
     */
    private void handleClientRequest(Socket clientSocket) throws IOException {
        try (
            // Create the input and output streams, to read the client request and send the file fragment
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream())); // to read the client request
            OutputStream out = clientSocket.getOutputStream()) {
    
            // Read the client request
            String request = in.readLine(); // Read the client request
            if (request == null || request.isEmpty()) {
                out.write("Invalid request\n".getBytes());
                return;
            }
    
            String[] parts = request.split(" "); // Split the request into parts
            if (parts.length < 2) {  // Check if the request is valid
                out.write("Invalid request\n".getBytes());
                return;
            }
    
            String command = parts[0];
            String fileName = parts[1];
            
            setFile(fileName);

            // we chose to have only two commands: SIZE and GET
            // SIZE: to get the size of the file (the receiver will know how many fragments to expect)
            // GET: to get a fragment of the file
            switch (command) {
                case "SIZE" -> {
                    String name = fileName + this.compressor.getExtension();
                    if (filePartage != null && filePartage.getName().equals(name)) {
                        out.write((filePartage.length() + "\n").getBytes());
                    } else {
                        out.write("File not found\n".getBytes());
                    }
                }
                case "CHECKSUM" -> {
                    String checksum = getChecksumFromDatabase(fileName);
                    out.write((checksum + "\n").getBytes());
                }
                case "FRAGMENT_CHECKSUM" -> {
                    if (parts.length != 4) {
                        out.write("FRAGMENT_CHECKSUM request invalid\n".getBytes());
                        return;
                    }
                    long startByte = Long.parseLong(parts[2]);
                    long endByte = Long.parseLong(parts[3]);
                    String fragmentChecksum = calculateFragmentChecksum(startByte, endByte);
                    out.write((fragmentChecksum + "\n").getBytes());
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

    private String getChecksumFromDatabase(String fileName) {
        String checksum = "";
        String selectSQL = "SELECT checksum FROM files WHERE file_name = ?";
        try (Connection connection = DriverManager.getConnection(urlDatabase);
             PreparedStatement selectStmt = connection.prepareStatement(selectSQL)) {
            selectStmt.setString(1, fileName);
            try (java.sql.ResultSet resultSet = selectStmt.executeQuery()) {
                if (resultSet.next()) {
                    checksum = resultSet.getString("checksum");
                }
            }
        } catch (SQLException e) {
            logError("SQL error: " + e.getMessage(), e);
        }
        return checksum;
    }
    
    /**
     * Send a fragment of the file to the client (from startByte to endByte)
     * @param out the output stream to send the fragment
     * @param startByte the start byte of the fragment
     * @param endByte the end byte of the fragment
     * @throws IOException if an error occurs during the communication
     */
    private void sendFileFragment(OutputStream out, long startByte, long endByte) throws IOException {
        try (
            // it's the way that I found to position the cursor at the startByte
            RandomAccessFile partFileToSend = new RandomAccessFile(filePartage, "r")) {
            partFileToSend.seek(startByte);  // position the cursor at the startByte
            long remainingBytes = endByte - startByte; // calculate the remaining bytes to send
            byte[] buffer = new byte[8192];
            int bytesRead;
            
            try {
                Thread.sleep(this.latency);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            while (remainingBytes > 0 && (bytesRead = partFileToSend.read(buffer, 0, (int) Math.min(buffer.length, remainingBytes))) != -1) {
                out.write(buffer, 0, bytesRead);
                remainingBytes -= bytesRead;
            }
            out.flush(); // to make sure that the fragment is sent
            log("Fragment sent successfully!");
        }
    }

    private String calculateFragmentChecksum(long startByte, long endByte) {
        try (
            RandomAccessFile partFileToSend = new RandomAccessFile(filePartage, "r")) {
            partFileToSend.seek(startByte);
            long remainingBytes = endByte - startByte;
            byte[] buffer = new byte[(int) remainingBytes];
            partFileToSend.readFully(buffer);
            HashCode hashCode = Hashing.sha256().hashBytes(buffer);
            return hashCode.toString();
        } catch (IOException e) {
            logError("Error calculating fragment checksum: " + e.getMessage(), e);
            return "";
        }
    }
}
