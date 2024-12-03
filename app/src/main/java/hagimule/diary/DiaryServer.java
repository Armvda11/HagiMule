package hagimule.diary;

import java.rmi.Naming;
import java.rmi.registry.LocateRegistry;


/**
 * Server class for the Diary service
 */
public class DiaryServer {
    public static void main(String[] args) {
        try {
            // Create and export a remote object
            DiaryImpl diary = new DiaryImpl();

            // Start the RMI registry on port 1099
            LocateRegistry.createRegistry(1099);

            // Bind the remote object's stub in the registry
            Naming.rebind("DiaryService", diary);

            System.out.println("DiaryService is running...");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}