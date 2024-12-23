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
import java.util.ArrayList;

import hagimule.diary.Diary;

public class ClientDownloader {

    private static final long MIN_FRAGMENT_SIZE = 512 * 1024; // Taille minimale d'un fragment (512 Ko)
    private static int maxConcurrentDownloads = 5;


    public static void main(String[] args) {
        try {

            String diaryAddress = (args.length > 0) ? args[0] : "localhost";
            String fileName = (args.length > 1) ? args[1] : "hagi.txt";

            // Se connecter au Diary via RMI
            Diary diary = (Diary) Naming.lookup("rmi://" + diaryAddress + "/Diary");
        
            System.out.println("Demande de téléchargement du fichier : " + fileName);

            // Récupérer la liste des Daemons qui possèdent le fichier
            List<String> daemonAddresses = diary.findDaemonAddressesByFile(fileName, maxConcurrentDownloads);
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
     * Télécharge les fragments du fichier et les écrit directement dans le fichier final.
     * 
     * @param fileName      Nom du fichier
     * @param daemonAddresses Adresses des Daemons
     * @param fileSize      Taille du fichier
     * @param outputFilePath Chemin du fichier final reconstitué
     */
    private static void downloadFragments(String fileName, List<String> daemonAddresses, long fileSize, String outputFilePath) throws IOException {
        int totalFragments = (int) Math.ceil((double) fileSize / MIN_FRAGMENT_SIZE);
        int nbThreads = Math.min(totalFragments, daemonAddresses.size() * 2);
        System.out.println("Nombre de Threads : " + nbThreads + " Nombre de fragments : " + totalFragments);

        String daemonAddressLent = daemonAddresses.get(0);
        ExecutorService executor = Executors.newCachedThreadPool();
        CompletionService<Void> completionService = new ExecutorCompletionService<>(executor);
        BlockingQueue<FragmentTask> fragmentQueue = new LinkedBlockingQueue<>();
        BlockingQueue<String> daemonQueue = new LinkedBlockingQueue<>(daemonAddresses);
        BlockingQueue<FragmentTask> currentlyProcessingQueue = new LinkedBlockingQueue<>();
        CountDownLatch latch = new CountDownLatch(totalFragments);
        ConcurrentLinkedQueue<FragmentResult> resultsQueue = new ConcurrentLinkedQueue<>();



        try (RandomAccessFile raf = new RandomAccessFile(outputFilePath, "rw")) {
            raf.setLength(fileSize);
        }

        for (int i = 0; i < totalFragments; i++) {
            long startByte = i * MIN_FRAGMENT_SIZE;
            long endByte = Math.min(startByte + MIN_FRAGMENT_SIZE, fileSize);
            fragmentQueue.add(new FragmentTask(i, startByte, endByte));
        }

        int nbRunningThreads = 0;
        int nbThreadsMax = 5;

        while ((latch.getCount() > 0) && nbRunningThreads < nbThreadsMax) {
            nbRunningThreads++;
            completionService.submit(() -> {
                while (true) {
                    FragmentTask task = fragmentQueue.poll();
                    if (task == null) {
                        if (latch.getCount() == 0) {
                            break;
                        }
                        task = currentlyProcessingQueue.poll();
                        if (task == null) {
                            break;
                        }
                        System.out.println("\n Fragment " + task.fragmentIndex + "  Volé !!! \n");
                    }
                    
                    currentlyProcessingQueue.add(task);
                    System.out.println("Fragment " + task.fragmentIndex + " en cours de traitement...");

                    String daemonAddress = daemonQueue.poll();
                    if (daemonAddress == null) {
                        break;
                    }
                    daemonQueue.add(daemonAddress);
                    try {
                        System.out.println("Fragment " + task.fragmentIndex + " début du téléchargement depuis :" + daemonAddress);
                        if (daemonAddressLent.equals(daemonAddress)) {
                            System.out.println("Debut sleep 50s : " + daemonAddress);
                            Thread.sleep(10000);
                            System.out.println("Fin sleep 50s : " + daemonAddress);
                        } else
                            {System.out.println("C'est l'autre");}

                        if ((latch.getCount() > 0)) {
                            byte[] data = downloadFragment(daemonAddress, fileName, task.startByte, task.endByte);
                            System.out.println("Fragment " + task.fragmentIndex + " téléchargé et écrit de " + task.startByte + " à " + (task.startByte + data.length) + " octets." + " téléchargé depuis :" + daemonAddress);
                            currentlyProcessingQueue.remove(task);

                            if ((latch.getCount() > 0)) {
                                Thread.sleep(10);
                                resultsQueue.add(new FragmentResult(task.startByte, data));
                                latch.countDown(); // Decrement the latch count
                                System.out.println(latch.getCount());
                                if (latch.getCount() == 0) {
                                    executor.shutdownNow();
                                }
                            } else {
                                break;
                            }
                        } else {
                            break;
                        }
                    } catch (IOException e) {
                        daemonQueue.remove(daemonAddress);
                        //e.printStackTrace();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
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

        for (FragmentResult result : resultsQueue) {
            try (RandomAccessFile raf = new RandomAccessFile(outputFilePath, "rw")) {
                raf.seek(result.getStartByte());
                raf.write(result.getData());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // Shutdown the executor service
        executor.shutdownNow(); // Interrupts all running tasks
        try {
            // Wait for all tasks to complete
            if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                executor.shutdownNow();
                if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                    System.err.println("Executor did not terminate");
                }
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

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
            String response = in.readLine();
            return Long.parseUnsignedLong(response.trim());
        } catch (IOException | NumberFormatException e) {
            e.printStackTrace();
        }
        return -1;
    }
}
