package hagimule.diary;

import java.rmi.Naming;
import java.rmi.registry.LocateRegistry;

/**
 * Main class to start the server
 * Here we create the RMI registry, on the default port 1099, and bind the Diary service to the registry
 */
public class DiaryServer {
    public static void main(String[] args) {
        try {
            // create the RMI registry
            LocateRegistry.createRegistry(1099);

            // Create a new instance of the Diary service
            Diary diary = new DiaryImpl();

            // Bind the Diary service to the registry
            Naming.rebind("rmi://localhost/Diary", diary);

            System.out.println("RMI Diary Server is running...");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}