package hagimule.client;

import java.net.InetAddress;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Scanner;
import java.util.UUID;

import hagimule.client.daemon.Daemon;
import hagimule.diary.Diary;

/**
 * Universal Client class
 * Each client creates a file and starts a Daemon that shares this file
 */
public class Machine {
    
    public static void main(String[] args) {
        try {
            // Reading execution parameters (file name, size, and port)
            String diaryAddress = (args.length > 0) ? args[0] : "localhost";
            int daemonPort = (args.length > 1) ? Integer.parseInt(args[1]) : 8080;
            String sharedFolder = (args.length > 2) ? args[2] : System.getProperty("user.dir") + "/shared/";

            String clientName ="Machine_" + UUID.randomUUID();
            System.out.println("Launching " + clientName);
            System.out.println("Connecting to Diary: " + diaryAddress);

            // Connecting to the Diary
            Diary diary = (Diary) Naming.lookup("rmi://"+ diaryAddress +"/Diary");
            
            // Starts the Daemon on the defined port

            System.out.println("Daemon port: " + daemonPort);
            Daemon daemon = new Daemon(daemonPort, sharedFolder);
            
            new Thread(daemon::start).start();

            // Registers the file and the Daemon in the Diary
            String machineIP = InetAddress.getLocalHost().getHostAddress();
            String daemonAddress = machineIP + ":" + daemonPort;
            
            synchronizeDiary(diary, daemon, clientName, daemonAddress);
            new Thread(() -> {
                while (true) {
                    try {
                        synchronizeDiary(diary, daemon, clientName, daemonAddress);
                        Thread.sleep(1000);
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
                String folderPath = "";
                if (folder.isEmpty()) {
                    folderPath = System.getProperty("user.dir") + "/received/";
                } else {
                    folderPath = System.getProperty("user.dir") + "/" + folder + "/";
                }
                System.out.println("Downloading file: " + fileName + " to " + folderPath);
                ClientDownloader.main(new String[]{diaryAddress, fileName, folderPath});
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
    
}
