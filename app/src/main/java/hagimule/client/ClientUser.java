package hagimule.client;

import java.net.InetAddress;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.util.List;
import java.util.UUID;

import hagimule.client.daemon.Daemon;
import hagimule.diary.Diary;

/**
 * Universal Client class
 * Each client creates a file and starts a Daemon that shares this file
 */
public class ClientUser {
    
    public static void main(String[] args) {
        try {
            // Reading execution parameters (file name, size, and port)
            String clientName = (args.length > 0) ? args[0] : "Client_" + UUID.randomUUID();
            int daemonPort = (args.length > 1) ? Integer.parseInt(args[1]) : 8080;
            String diaryAddress = (args.length > 2) ? args[2] : "localhost";

            System.out.println("Launching " + clientName);
            System.out.println("Connecting to Diary: " + diaryAddress);

            // Connecting to the Diary (the diary 147.27.133.14 (pixie))
            Diary diary = (Diary) Naming.lookup("rmi://"+ diaryAddress +"/Diary");
            
            // Starts the Daemon on the defined port

            System.out.println("Daemon port: " + daemonPort);
            Daemon daemon = new Daemon(daemonPort);
            
            new Thread(daemon::start).start();

            // Registers the file and the Daemon in the Diary
            String machineIP = InetAddress.getLocalHost().getHostAddress();
            String daemonAddress = machineIP + ":" + daemonPort;

            synchronizeDiary(diary, daemon, clientName, daemonAddress);

            System.out.println("Daemon listening on: " + daemonAddress);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void synchronizeDiary(Diary diary, Daemon daemon, String ownerName, String ownerAddress) {
        List<String> filesNames;
    
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
