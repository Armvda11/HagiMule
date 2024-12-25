package hagimule.diary;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.Naming;
import java.rmi.registry.LocateRegistry;

/**
 * Main class to start the server
 * Here we create the RMI registry, on the default port 1099, and bind the Diary service to the registry
 */
public class DiaryServer {
    public static void main(String[] args) {
        try {
            // Reading execution parameters (diary address and port)
            String diaryAddress = (args.length > 0) ? args[0] : "localhost";
            int diaryPort = (args.length > 1) ? Integer.parseInt(args[1]) : 1099;

            System.setProperty("java.rmi.server.hostname", diaryAddress);
            // create the RMI registry
            LocateRegistry.createRegistry(diaryPort);

             /* Suite :
             * ip de la machine local : InetAddress.getLocalHost().getHostAddress()
             *  Naming.rebind("rmi://" + Ip_machine_local + "/Diary", diary); 
             */

             try {
                InetAddress localMachine = InetAddress.getLocalHost();
                String adresseIP = localMachine.getHostAddress();
               
                System.out.println("Adresse IP de la machine locale : " + adresseIP);
            } catch (UnknownHostException e) {
                System.err.println("Impossible de récupérer l'adresse IP.");
                e.printStackTrace();
            }
            
            // Create a new instance of the Diary service
            Diary diary = new DiaryImpl();
           

            // Bind the Diary service to the registry
            // le server pixie est sur l'adresse 147.127.133.14
            Naming.rebind("rmi://"+ diaryAddress + ":1099/Diary", diary);

            System.out.println("RMI Diary Server is running...");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}   