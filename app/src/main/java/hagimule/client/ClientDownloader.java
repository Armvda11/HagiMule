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

import hagimule.diary.Diary;

public class ClientDownloader {

    private static final long MIN_FRAGMENT_SIZE = 512 * 1024; // Taille minimale d'un fragment (512 Ko)
    // Max number of concurrent sources to download from
    private static int maxConcurrentSources = 5;


    public static void main(String[] args) {
        try {

            String diaryAddress = (args.length > 0) ? args[0] : "localhost";
            String fileName = (args.length > 1) ? args[1] : "hagi.txt";
            String receivedFolderPath = (args.length > 2) ? args[2] : System.getProperty("user.dir") + "\\received\\";
            String sharedFolderPath = System.getProperty("user.dir") + "\\shared\\";
            // Se connecter au Diary via RMI
            Diary diary = (Diary) Naming.lookup("rmi://" + diaryAddress + "/Diary");
        
            System.out.println("Demande de téléchargement du fichier : " + fileName);

            // Récupérer la liste des Daemons qui possèdent le fichier
            List<String> daemonAddresses = diary.findDaemonAddressesByFile(fileName, maxConcurrentSources);
            if (daemonAddresses.isEmpty()) {
                System.out.println("Aucun Daemon ne possède ce fichier.");
                return;
            }

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

            // Remplacez par FileCompressorLZMA si nécessaire
            FileCompressor compressor = new FileCompressorZstd(22);

            Path SharedFilePath = Paths.get(sharedFolderPath + fileName + compressor.getExtension());
            // Télécharger les fragments et reconstituer le fichier
            downloadFragments(fileName, daemonAddresses, fileSize, SharedFilePath.toString());

            Path reveivedFilePath = Paths.get(receivedFolderPath + fileName);

            Path decompressedFile = compressor.decompressFile(SharedFilePath, reveivedFilePath);
            System.out.println("Fichier décompressé : " + decompressedFile);
            System.out.println("Fichier reconstitué avec succès : " + fileName);
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
        // Calculate the total number of fragments
        int totalFragments = (int) Math.ceil((double) fileSize / MIN_FRAGMENT_SIZE);
        
        // Number of daemon addresses available
        int nbAdresses = daemonAddresses.size();
        
        // Determine the number of threads to use
        int nbThreads = Math.min(totalFragments, nbAdresses * 2);
        System.out.println("Nombre de Threads : " + nbThreads + " Nombre de fragments : " + totalFragments);

        // Address of the daemon that is slow (for testing purpose)
        String daemonAddressLent = daemonAddresses.get(0);

        // Executor service to manage threads
        ExecutorService executor = Executors.newCachedThreadPool();
        
        // Completion service to handle task completion
        CompletionService<Void> completionService = new ExecutorCompletionService<>(executor);
        
        // Queue to hold fragment tasks
        BlockingQueue<FragmentTask> fragmentQueue = new LinkedBlockingQueue<>();
        
        // Queue to hold daemon addresses
        BlockingQueue<String> daemonQueue = new LinkedBlockingQueue<>(daemonAddresses);
        
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
        for (String address : daemonAddresses) {
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
                            System.out.println("Debut sleep 50s : " + daemonAddress);
                            Thread.sleep(200);
                            System.out.println("Fin sleep 50s : " + daemonAddress);
                        } else
                            {System.out.println("C'est l'autre");}

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
            return byteArrayOutputStream.toByteArray();
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
            System.out.println("Demande de taille du fichier " + fileName + " à " + daemonAddress);
            String response = in.readLine();
            return Long.parseUnsignedLong(response.trim());
        } catch (IOException | NumberFormatException e) {
            e.printStackTrace();
        }
        return -1;
    }
}