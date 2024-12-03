package hagimule.diary;

import java.rmi.Naming;
import java.rmi.registry.LocateRegistry;

public class DiaryServer {
    public static void main(String[] args) {
        try {
            // Crée le registre RMI
            LocateRegistry.createRegistry(1099);

            // Instancie le service Diary
            Diary diary = new DiaryImpl();

            // Lie le service Diary au registre RMI
            Naming.rebind("rmi://localhost/Diary", diary);

            System.out.println("RMI Diary Server is running...");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}