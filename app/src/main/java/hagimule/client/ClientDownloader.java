package hagimule.client;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.net.ConnectException;
import java.net.Socket;
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
import java.util.concurrent.TimeUnit;
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

            // Se connecter au Diary via RMI
            Diary diary = (Diary) Naming.lookup("rmi://" + diaryAddress + "/Diary");

            System.out.println("Demande de téléchargement du fichier : " + fileName);

            // Récupérer la liste des Daemons qui possèdent le fichier
            List<String> daemonAddresses = diary.findDaemonAddressesByFile(fileName, maxConcurrentSources);
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
     * downloadFragments : download the fragments of a file from multiple Daemons and reassemble them.
     * @param fileName      Name of the file
     * @param daemonAddresses   List of Daemon addresses
     * @param fileSize          Size of the file
     * @param outputFilePath        Path of the output file
     * @throws IOException
     */
    private static void downloadFragments(String fileName, List<String> daemonAddresses, long fileSize,
            String outputFilePath) throws IOException {
        // Calculate the total number of fragments
        int totalFragments = (int) Math.ceil((double) fileSize / MIN_FRAGMENT_SIZE);
        System.out.println("Total number of fragments: " + totalFragments);

        // Create the output file with the correct size
        createOutputFile(outputFilePath, fileSize);
        System.out.println("Output file created at: " + outputFilePath);

        // Initialize queues
        BlockingQueue<FragmentTask> fragmentQueue = initializeFragmentQueue(totalFragments, fileSize); // Queue of fragments to download
        BlockingQueue<String> daemonQueue = new LinkedBlockingQueue<>(daemonAddresses); // Queue of daemon addresses
        Map<String, Integer> addressUsageCount = initializeAddressUsageCount(daemonAddresses); // Map of address to usage count

        // Executor service and latch for task completion
        ExecutorService executor = Executors.newCachedThreadPool(); // Executor service
        CountDownLatch latch = new CountDownLatch(totalFragments); // Latch to wait for all fragments to be processed
        ConcurrentLinkedQueue<FragmentResult> resultsQueue = new ConcurrentLinkedQueue<>(); // Queue to store fragment results

        // Submit download tasks
        submitDownloadTasks(executor, fragmentQueue, daemonQueue, addressUsageCount, fileName, latch, resultsQueue);

        // Wait for all fragments to be processed
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Write the fragments to the output file
        writeFragmentsToFile(outputFilePath, resultsQueue);
    }

    /**
     * Create an output file with the specified size.
     * @param outputFilePath Path of the output file
     * @param fileSize      Size of the file
     * @throws IOException
     */
    private static void createOutputFile(String outputFilePath, long fileSize) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(outputFilePath, "rw")) {
            raf.setLength(fileSize);
        }
    }

    /**
     * Initialize the fragment queue with the start and end bytes of each fragment.
     * @param totalFragments    Total number of fragments
     * @param fileSize          Size of the file
     * @return Queue of fragment tasks
     */
    private static BlockingQueue<FragmentTask> initializeFragmentQueue(int totalFragments, long fileSize) {
        BlockingQueue<FragmentTask> fragmentQueue = new LinkedBlockingQueue<>();
        for (int i = 0; i < totalFragments; i++) {
            long startByte = i * MIN_FRAGMENT_SIZE;
            long endByte = Math.min(startByte + MIN_FRAGMENT_SIZE, fileSize);
            fragmentQueue.add(new FragmentTask(i, startByte, endByte));
            System.out.println("Fragment " + i + " initialized: StartByte=" + startByte + ", EndByte=" + endByte);
        }
        return fragmentQueue;
    }

    /**
     * Initialize the address usage count map with the daemon addresses.
     * @param daemonAddresses   List of daemon addresses
     * @return Map of address to usage count
     */
    private static Map<String, Integer> initializeAddressUsageCount(List<String> daemonAddresses) {
        // Initialize the address usage count map
        Map<String, Integer> addressUsageCount = new HashMap<>();
        // Initialize the usage count for each Daemon address
        for (String address : daemonAddresses) {
            addressUsageCount.put(address, 0);
            System.out.println("Daemon address initialized: " + address);
        }
        return addressUsageCount;
    }

    /**
     * Submit download tasks to the executor service.
     * @param executor          Executor service
     * @param fragmentQueue     Queue of fragment tasks
     * @param daemonQueue       Queue of daemon addresses
     * @param addressUsageCount Map of address to usage count
     * @param fileName          Name of the file
     * @param latch             CountDownLatch for task completion
     * @param resultsQueue      Queue to store fragment results
     */
    private static void submitDownloadTasks(ExecutorService executor, BlockingQueue<FragmentTask> fragmentQueue,
            BlockingQueue<String> daemonQueue,
            Map<String, Integer> addressUsageCount, String fileName, CountDownLatch latch,
            ConcurrentLinkedQueue<FragmentResult> resultsQueue) {
        // Submit download tasks to the executor service
        for (int i = 0; i < Math.min(fragmentQueue.size(), 5); i++) {
            executor.submit(() -> {
                // Process tasks until the latch is zero
                while (latch.getCount() > 0) {
                    // Get the next fragment task
                    FragmentTask task = fragmentQueue.poll();
                    if (task == null) {
                        break;
                    }

                    // Select a Daemon to download the fragment from
                    String daemonAddress = selectDaemonForTask(daemonQueue, addressUsageCount);
                    if (daemonAddress == null) {
                        break;
                    }
                    
                    System.out.println("Downloading fragment " + task.fragmentIndex + " from Daemon: " + daemonAddress);

                    // Download the fragment from the Daemon
                    try {
                        byte[] data = downloadFragment(daemonAddress, fileName, task.startByte, task.endByte);
                        resultsQueue.add(new FragmentResult(task.startByte, data));
                        latch.countDown();
                    } catch (IOException e) {
                        // Handle download failure (optional logging or retry logic)
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    /**
     * Write the fragments to the output file.
     * @param outputFilePath    Path of the output file
     * @param resultsQueue      Queue of fragment results
     * @throws IOException
     */
    private static void writeFragmentsToFile(String outputFilePath, ConcurrentLinkedQueue<FragmentResult> resultsQueue)
            throws IOException {
        for (FragmentResult result : resultsQueue) {
            try (RandomAccessFile raf = new RandomAccessFile(outputFilePath, "rw")) {
                raf.seek(result.getStartByte());
                raf.write(result.getData());
                System.out.println("Fragment written to file: StartByte=" + result.getStartByte());
            }
        }
    }

    /**
     * Select a Daemon to download a fragment from.
     * 
     * @param daemonQueue       Queue of Daemon addresses
     * @param addressUsageCount Map of address to usage count
     * @return Daemon address or null if no Daemon is available
     */
    private static String selectDaemonForTask(BlockingQueue<String> daemonQueue,
            Map<String, Integer> addressUsageCount) {
        
        // Get the next Daemon address from the queue
        String daemonAddress = daemonQueue.poll();
        // If the Daemon is overused or unavailable, return null
        // if (daemonAddress == null || addressUsageCount.get(daemonAddress) >= 3) {
        if (daemonAddress == null) {
            System.out.println("Daemon overused or unavailable: " + daemonAddress);
            return null; // This daemon is overused or unavailable
        }
        // Add the Daemon back to the queue and increment the usage count
        daemonQueue.add(daemonAddress);
        addressUsageCount.put(daemonAddress, addressUsageCount.get(daemonAddress) + 1);
        System.out.println("Selected Daemon for fragment: " + daemonAddress);
        return daemonAddress;
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
     * @return Les octets du fragment
     */
    private static byte[] downloadFragment(String daemonAddress, String fileName, long startByte, long endByte)
            throws IOException {
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
     * @return Taille du fichier
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
