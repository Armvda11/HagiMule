package hagimule.client;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.net.Socket;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.Naming;
import java.rmi.Registry;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import com.google.common.hash.Hashing;
import com.google.common.hash.HashCode;

import hagimule.client.Compressor.FileCompressor;
import hagimule.client.Compressor.FileCompressorZstd;
import hagimule.diary.Diary;
import hagimule.client.Compressor.FileCompressorLZ4;
import hagimule.client.Compressor.FileCompressorLZMA;
import hagimule.client.Compressor.FileCompressorVide;
import hagimule.client.daemon.Daemon;
import java.rmi.registry.LocateRegistry;

public class ClientDownloader {

    private static final long MIN_FRAGMENT_SIZE = 512 * 1024; // Taille minimale d'un fragment (512 Ko)
    // Max number of concurrent sources to download from
    private static int maxConcurrentSources = 5;
    private static String expectedChecksum;
    private static FileCompressor compressor = new FileCompressorZstd(22);
    private static String receivedFolderPath;


    public static void main(String[] args) {
        BlockingQueue<String> daemonQueue = new LinkedBlockingQueue<>();
        try {
            String diaryAddress = (args.length > 0) ? args[0] : "localhost";
            String fileName = (args.length > 1) ? args[1] : "hagi.txt";
            receivedFolderPath = (args.length > 2) ? args[2] : System.getProperty("user.dir") + "/received/";
            if (args.length > 3) {
                maxConcurrentSources = Integer.parseInt(args[3]);
            }
            String sharedFolderPath = (args.length > 4) ? args[4] : System.getProperty("user.dir") + "/shared/";
            // Se connecter au Diary via RMI
            // Diary diary = (Diary) Naming.lookup("rmi://" + diaryAddress + "/Diary");
            Registry registry= LocateRegistry.getRegistry(diaryAddress, diaryPort);
            Diary diary = (Diary) registry.lookup("Diary");
        
            System.out.println("Demande de téléchargement du fichier : " + fileName);

            // Récupérer la liste des Daemons qui possèdent le fichier
            List<String> daemonAddresses = diary.findDaemonAddressesByFile(fileName, maxConcurrentSources);
            if (daemonAddresses.isEmpty()) {
                System.out.println("Aucun Daemon ne possède ce fichier.");
                return;
            }

            daemonQueue.addAll(daemonAddresses);

            File directory = new File(receivedFolderPath);
            if (!directory.exists()) {
                directory.mkdirs();
            }

            System.out.println(daemonAddresses);

            // Obtenir la taille du fichier à partir d'un des Daemons
            long fileSize = getFileSize(daemonAddresses.get(0), fileName);
            if (fileSize <= 0) {
                System.out.println("Impossible d'obtenir la taille du fichier.");
                return;
            }

            // Obtenir le checksum attendu à partir d'un des Daemons
            expectedChecksum = getChecksum(daemonAddresses.get(0), fileName);
            if (expectedChecksum.isEmpty()) {
                System.out.println("Impossible d'obtenir le checksum du fichier.");
                return;
            }
            System.out.println("Checksum attendu : " + expectedChecksum);

            // Get the compressor type from the Daemon
            String compressorType = getCompressorType(daemonAddresses.get(0), fileName);
            if (compressorType.isEmpty()) {
                System.out.println("Impossible d'obtenir le type de compresseur du fichier.");
                return;
            }
            System.out.println("Type de compresseur : " + compressorType);

            compressor = createCompressor(compressorType);

            Path SharedFilePath = Paths.get(sharedFolderPath + fileName + compressor.getExtension());
            // Télécharger les fragments et reconstituer le fichier
            downloadFragments(fileName, daemonQueue, fileSize, SharedFilePath.toString());

            // Timer to update daemonQueue every 5 seconds
            // The downloader always gets the least used Daemons from the Diary to avoid overloading a single Daemon
            Timer timer = new Timer(true);
            timer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    try {
                        if (daemonQueue.size() < maxConcurrentSources) {
                            List<String> updatedDaemonAddresses = Machine.getUpdatedDaemonAddresses(diary, fileName, maxConcurrentSources);
                            for (String address : updatedDaemonAddresses) {
                                if (!daemonQueue.contains(address)) {
                                    daemonQueue.add(address);
                                }
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }, 0, 5000);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static FileCompressor createCompressor(String compressorType) {
        switch (compressorType.toLowerCase()) {
            case "lz4":
                return new FileCompressorLZ4();
            case "lzma":
                return new FileCompressorLZMA();
            case "vide":
                return new FileCompressorVide();
            case "zstd":
            default:
                System.out.println("Compresseur par défaut : Zstd");
                return new FileCompressorZstd(22); // Default compressor
        }
    }

    private static String getCompressorType(String daemonAddress, String fileName) {
        String[] parts = daemonAddress.split(":");
        String host = parts[0];
        int port = Integer.parseInt(parts[1]);

        try (Socket socket = new Socket(host, port);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            out.println("COMPRESSOR_TYPE " + fileName);
            return in.readLine().trim();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }

    /**
     * Télécharge les fragments du fichier et les écrit directement dans le fichier final.
     * 
     * @param fileName      Nom du fichier
     * @param daemonQueue   Queue des Daemons
     * @param fileSize      Taille du fichier
     * @param outputFilePath Chemin du fichier final reconstitué
     */
    private static void downloadFragments(String fileName, BlockingQueue<String> daemonQueue, long fileSize, String outputFilePath) throws IOException {
        // Calculate the total number of fragments
        int totalFragments = (int) Math.ceil((double) fileSize / MIN_FRAGMENT_SIZE);
        
        // Number of daemon addresses available
        int nbAdresses = daemonQueue.size();
        
        // Determine the number of threads to use
        // int nbThreads = Math.min(totalFragments, nbAdresses * 2);
        // One thread per source, even if there are less fragments, we can steal fragments from other sources so we can use all sources
        int nbThreads = maxConcurrentSources;
        System.out.println("Nombre de Threads : " + nbThreads + " Nombre de fragments : " + totalFragments);

        // Address of the daemon that is slow (for testing purpose)
        String daemonAddressLent = daemonQueue.peek();

        // Executor service to manage threads
        ExecutorService executor = Executors.newCachedThreadPool();
        
        // Completion service to handle task completion
        CompletionService<Void> completionService = new ExecutorCompletionService<>(executor);
        
        // Queue to hold fragment tasks
        BlockingQueue<FragmentTask> fragmentQueue = new LinkedBlockingQueue<>();
        
        // Queue to hold currently processing fragment tasks
        BlockingQueue<FragmentTask> currentlyProcessingQueue = new LinkedBlockingQueue<>();
        
        // Latch to wait for all fragments to be processed
        // Once all fragments are processed, the latch count will be 0
        // And the executor service will be shutdown
        CountDownLatch latch = new CountDownLatch(totalFragments);
        
        // Queue to hold results of fragment downloads
        ConcurrentLinkedQueue<FragmentResult> resultsQueue = new ConcurrentLinkedQueue<>();
        
        // Map to keep track of the usage count of each address
        // distribute the load or avoid to use a slow source
        Map<String, Integer> addressUsageCount = new HashMap<>();
        
        // Initialize the address usage count
        for (String address : daemonQueue) {
            addressUsageCount.put(address, 0);
        }

        // Create the output file with the correct size
        try (RandomAccessFile raf = new RandomAccessFile(outputFilePath, "rw")) {
            raf.setLength(fileSize);
        }

        // Create the fragment tasks and add them to the queue
        for (int i = 0; i < totalFragments; i++) {
            long startByte = i * MIN_FRAGMENT_SIZE;
            long endByte = Math.min(startByte + MIN_FRAGMENT_SIZE, fileSize);
            fragmentQueue.add(new FragmentTask(i, startByte, endByte));
        }
        
        // Count the actual number of threads running
        int nbRunningThreads = 0;
        // Maximum number of threads that can run at the same time
        int nbRunningThreadsMax = nbThreads;
        // Maximum number of uses of an address
        // To avoid overusing a source
        int nbMaxUsesAdress = nbRunningThreadsMax / nbAdresses;
        while ((latch.getCount() > 0) && nbRunningThreads < nbRunningThreadsMax) {
            nbRunningThreads++;
            completionService.submit(() -> {
                while (latch.getCount() != 0) {
                    System.out.println(addressUsageCount);
                    System.out.println("Thread en cours : " + Thread.currentThread().getName());

                    // Get the next fragment task to process
                    FragmentTask task = fragmentQueue.poll();
                    if (task == null) {
                        // If there are no more fragments to process, check if
                        // there are any currently processing fragments to steal
                        task = currentlyProcessingQueue.poll();
                        if (task == null) {
                            break;
                        }
                        System.out.println("\n Fragment " + task.fragmentIndex + "  Volé !!! \n");
                    }
                    
                    // Add the task to the currently processing queue
                    currentlyProcessingQueue.add(task);
                    System.out.println("Fragment " + task.fragmentIndex + " en cours de traitement...");
                    
                    int nbTries = 0;

                    // Get the next daemon address to use
                    String daemonAddress = daemonQueue.poll();
                    daemonQueue.add(daemonAddress);
                    // Check if the address has been used too many times
                    // If every address is overused, we chose the first one
                    while (addressUsageCount.get(daemonAddress) >= nbMaxUsesAdress && nbTries < nbAdresses) {
                        nbTries++;
                        daemonAddress = daemonQueue.poll();
                        daemonQueue.add(daemonAddress);
                    }
                    if (daemonAddress == null) {
                        System.out.println("Pas de Daemon disponible pour le fragment " + task.fragmentIndex);
                        break;
                    }
                    // Increment the usage count of the address
                    addressUsageCount.put(daemonAddress, addressUsageCount.get(daemonAddress) + 1);

                    // Process the fragment
                    try {
                        System.out.println("Fragment " + task.fragmentIndex + " début du téléchargement depuis :" + daemonAddress);

                        // For testing purpose, we simulate a slow daemon
                        if (daemonAddressLent.equals(daemonAddress)) {
                            System.out.println("Debut sleep : " + daemonAddress);
                            Thread.sleep(200);
                            System.out.println("Fin sleep : " + daemonAddress);
                        }

                        // Download the fragment
                        byte[] data = downloadFragment(daemonAddress, fileName, task.startByte, task.endByte);
                        
                        addressUsageCount.put(daemonAddress, addressUsageCount.get(daemonAddress) - 1);
                        
                        System.out.println("Fragment " + task.fragmentIndex + " téléchargé et écrit de " + task.startByte + " à " + (task.startByte + data.length) + " octets." + " téléchargé depuis :" + daemonAddress);

                        // Add the fragment result to the results queue
                        resultsQueue.add(new FragmentResult(task.startByte, data));

                        // We remove the task from the currently processing queue as it is done
                        currentlyProcessingQueue.remove(task);
                        latch.countDown(); // Decrement the latch count
                        System.out.println(latch.getCount());

                        // If all fragments are processed, shutdown the executor
                        if (latch.getCount() == 0) {
                            executor.shutdownNow();
                        }
                        
                    } catch (IOException e) {
                        daemonQueue.remove(daemonAddress);
                        System.out.println("Source déconnectée : " + daemonAddress);
                        addressUsageCount.put(daemonAddress, addressUsageCount.get(daemonAddress) - 1);
                        //e.printStackTrace();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        addressUsageCount.put(daemonAddress, addressUsageCount.get(daemonAddress) - 1);
                        System.out.println("Thread interrompu : " + Thread.currentThread().getName());
                        break;
                    }
                }
                System.out.println("Thread termine : " + Thread.currentThread().getName());
                return null;
            });
        }

        // Wait for all fragments to be processed
        try {
            latch.await();
            // We wait for everything to be written to the file
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Write the fragments to the output file
        for (FragmentResult result : resultsQueue) {
            try (RandomAccessFile raf = new RandomAccessFile(outputFilePath, "rw")) {
                raf.seek(result.getStartByte());
                raf.write(result.getData());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // Verify the checksum of the fully downloaded file
        File downloadedFile = new File(outputFilePath);
        String actualChecksum = calculateChecksum(downloadedFile);
        if (expectedChecksum.equals(actualChecksum)) {
            System.out.println("Checksum vérifié avec succès pour le fichier compressé : " + fileName);
            Path receivedFilePath = Paths.get(receivedFolderPath + fileName);
            Path decompressedFile = compressor.decompressFile(downloadedFile.toPath(), receivedFilePath);
            System.out.println("Fichier décompressé : " + decompressedFile);
        } else {
            System.out.println("Le checksum du fichier compressé téléchargé ne correspond pas au checksum attendu. Le fichier est corrompu. \n Le checksum attendu est : " + expectedChecksum + "\n Le checksum calculé est : " + actualChecksum);
        }
    }

    // Intern classes to handle fragment tasks and results
    private static class FragmentTask {
        int fragmentIndex;
        long startByte;
        long endByte;

        FragmentTask(int fragmentIndex, long startByte, long endByte) {
            this.fragmentIndex = fragmentIndex;
            this.startByte = startByte;
            this.endByte = endByte;
        }
    }

    // Intern class to handle fragment results
    private static class FragmentResult {
        long startByte;
        byte[] data;

        FragmentResult(long startByte, byte[] data) {
            this.startByte = startByte;
            this.data = data;
        }

        public long getStartByte() {
            return startByte;
        }

        public byte[] getData() {
            return data;
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
            
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                byteArrayOutputStream.write(buffer, 0, bytesRead);
            }
            byte[] data = byteArrayOutputStream.toByteArray();

            // Verify fragment checksum
            String receivedChecksum = getFragmentChecksum(daemonAddress, fileName, startByte, endByte);
            String calculatedChecksum = calculateChecksum(data);
            if (!receivedChecksum.equals(calculatedChecksum)) {
                if (receivedChecksum.equals("")) {
                    System.out.println("Checksum vide donc ignoré");
                } else {
                    System.out.println("Checksum invalide");
                    throw new IOException("Checksum mismatch");
                }
            } else {
                System.out.println("Checksum OK");
            }
            return data;
        }
    }

    private static String getFragmentChecksum(String daemonAddress, String fileName, long startByte, long endByte) throws IOException {
        String[] parts = daemonAddress.split(":");
        String host = parts[0];
        int port = Integer.parseInt(parts[1]);

        try (Socket socket = new Socket(host, port);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            out.println("FRAGMENT_CHECKSUM " + fileName + " " + startByte + " " + endByte);
            return in.readLine().trim();
        }
    }

    private static String calculateChecksum(byte[] data) {
        HashCode hashCode = Hashing.sha256().hashBytes(data);
        return hashCode.toString();
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
            System.out.println("Demande de taille du fichier " + fileName + " à " + daemonAddress);
            String response = in.readLine();
            return Long.parseUnsignedLong(response.trim());
        } catch (IOException | NumberFormatException e) {
            e.printStackTrace();
        }
        return -1;
    }

    /**
     * Récupère le checksum du fichier auprès d'un Daemon.
     * 
     * @param daemonAddress Adresse du Daemon
     * @param fileName      Nom du fichier
     * @return              Checksum du fichier
     */
    private static String getChecksum(String daemonAddress, String fileName) {
        String[] parts = daemonAddress.split(":");
        String host = parts[0];
        int port = Integer.parseInt(parts[1]);

        try (Socket socket = new Socket(host, port);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            out.println("CHECKSUM " + fileName);
            System.out.println("Demande de checksum du fichier " + fileName + " à " + daemonAddress);
            return in.readLine().trim();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }

    /**
     * Calcule le checksum d'un fichier.
     * 
     * @param file Fichier dont le checksum doit être calculé
     * @return     Checksum du fichier
     */
    private static String calculateChecksum(File file) {
        return Daemon.calculateChecksum(file);
    }
}