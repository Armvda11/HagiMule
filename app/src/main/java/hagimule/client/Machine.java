package hagimule.client;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.InetAddress;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Scanner;

import hagimule.client.daemon.Daemon;
import hagimule.diary.Diary;

/**
 * Universal Client class
 * Each client creates a file and starts a Daemon that shares this file
 */
public class Machine {
    static PrintStream logTime;
    static String clientName;
    public static void main(String[] args) {
        try {
            // Reading execution parameters (file name, size, and port)
            String diaryAddress = (args.length > 0) ? args[0] : "localhost";
            int daemonPort = (args.length > 1) ? Integer.parseInt(args[1]) : 8080;
            String sharedFolder = (args.length > 2) ? args[2] : System.getProperty("user.dir") + "/shared/";
            String defaultReceivedFolder = (args.length > 3) ? args[3] : System.getProperty("user.dir") + "/received/";
            String compressor = (args.length > 4) ? args[4] : "defaultCompressor";
            int latency = (args.length > 5) ? Integer.parseInt(args[5]) : 1000;
            String maxSources = (args.length > 6) ? args[6] : "5";

            clientName = "Client_" + java.net.InetAddress.getLocalHost().getHostName() + "_" + daemonPort;
            System.out.println("Launching " + clientName);
            System.out.println("Connecting to Diary: " + diaryAddress);
            setupLogging();
            // Connecting to the Diary
            Diary diary = (Diary) Naming.lookup("rmi://"+ diaryAddress +"/Diary");
            
            // Starts the Daemon on the defined port

            System.out.println("Daemon port: " + daemonPort);
            Daemon daemon = new Daemon(daemonPort, sharedFolder, compressor, latency);
            
            new Thread(daemon::start).start();

            // Registers the file and the Daemon in the Diary
            String machineIP = InetAddress.getLocalHost().getHostAddress();
            String daemonAddress = machineIP + ":" + daemonPort;
            
            synchronizeDiary(diary, daemon, clientName, daemonAddress);
            new Thread(() -> {
                while (true) {
                    try {
                        synchronizeDiary(diary, daemon, clientName, daemonAddress);
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }).start();

            System.out.println("Daemon listening on: " + daemonAddress);

            // Prompt user to download files
            Scanner sc = new Scanner(System.in);
            while (true) {
                System.out.println("Enter a file name to download or 'exit' to quit:");
                String fileName = sc.nextLine();
                if ("exit".equalsIgnoreCase(fileName.trim())) {
                    break;
                }
                System.out.println("Enter the folder path to save the file (press Enter for default):");
                String folder = sc.nextLine();
                String receivedfolderPath = "";
                if (folder.isEmpty()) {
                    receivedfolderPath = defaultReceivedFolder;
                } else {
                    receivedfolderPath = System.getProperty("user.dir") + "/" + folder + "/";
                }
                System.out.println("Downloading file: " + fileName + " to " + receivedfolderPath);
                
                long startTime = System.currentTimeMillis();
                ClientDownloader.main(new String[]{diaryAddress, fileName, receivedfolderPath, maxSources, sharedFolder});
                long endTime = System.currentTimeMillis();
                long duration = endTime - startTime;
                String textTime = "Downloaded file: " + fileName + " in " + duration + " ms";
                System.out.println(textTime);
                log(textTime);
            }
            sc.close();
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void synchronizeDiary(Diary diary, Daemon daemon, String ownerName, String ownerAddress) {
        List<String> filesNames;
    
        daemon.compressSharedFiles();
        daemon.updateDatabase();

        filesNames = daemon.getFilesNames();
        
        for (String fileName : filesNames) {
            try {
                diary.registerFile(fileName, ownerName, ownerAddress);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }
    private static void setupLogging() {
        try {
            String logFilePath = System.getProperty("user.dir") + "/logs/logTime_" + clientName + ".txt";
            logTime = new PrintStream(new FileOutputStream(logFilePath, true), true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void log(String message) {
        logTime.println(message);
    }
    
}
