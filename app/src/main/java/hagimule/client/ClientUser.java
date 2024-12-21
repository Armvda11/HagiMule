package hagimule.client;

import java.net.InetAddress;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.util.List;
import java.util.UUID;

import hagimule.client.daemon.Daemon;
import hagimule.diary.Diary;

/**
 * Classe Client universelle
 * Chaque client crée un fichier et démarre un Daemon qui partage ce fichier
 */
public class ClientUser {
    
    public static void main(String[] args) {
        try {
            // Lecture des paramètres d'exécution (nom du fichier, taille et port)
            String clientName = (args.length > 0) ? args[0] : "Client_" + UUID.randomUUID();
            int daemonPort = (args.length > 1) ? Integer.parseInt(args[1]) : 8080;
            String diaryAddress = (args.length > 2) ? args[2] : "localhost";

            System.out.println("Lancement de " + clientName);
            System.out.println("Connexion au Diary : " + diaryAddress);


            // Connexion au Diary ( le diary 147.27.133.14   ( pixie ))
            Diary diary = (Diary) Naming.lookup("rmi://"+ diaryAddress +"/Diary");
            
            // Démarre le Daemon sur le port défini

            System.out.println("Port du Daemon : " + daemonPort);
            Daemon daemon = new Daemon(daemonPort);
            
            new Thread(daemon::start).start();

            // Enregistre le fichier et le Daemon dans le Diary
            String machineIP = InetAddress.getLocalHost().getHostAddress();
            String daemonAddress = machineIP + ":" + daemonPort;

            synchroniserDiary(diary, daemon, clientName, daemonAddress);

            System.out.println("Daemon à l'écoute sur : " + daemonAddress);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void synchroniserDiary(Diary diary, Daemon daemon, String ownerName, String ownerAddress) {
        List<String> filesNames;
    
        daemon.majDatabase();

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
