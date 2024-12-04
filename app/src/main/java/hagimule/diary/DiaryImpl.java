package hagimule.diary;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implemantation of the Diary interface
 * this class is used to register and find files for clients
 */
public class DiaryImpl extends UnicastRemoteObject implements Diary {

    // Map to store the files of each client , with the file name as key and the list of clients as value
    private final Map<String, List<ClientInfo>> fileToClients = new HashMap<>();

    // Constructor
    public DiaryImpl() throws RemoteException {
        super();
    }

    /**
     * Register a file for a specific client.
     */
    @Override
    public void registerFile(String fileName, String clientName, String daemonAddress) throws RemoteException {
        // Si le fichier n'existe pas encore, crée une nouvelle liste pour les clients
        fileToClients.computeIfAbsent(fileName, k -> new ArrayList<>())
                     .add(new ClientInfo(clientName, daemonAddress));
        System.out.println("Enregistrement du fichier : " + fileName + " pour le client : " + clientName +
                           " à l'adresse : " + daemonAddress);
    }

    /**
     * Find all clients who own a specific file.
     */
    @Override
    public List<String> findClientsByFile(String fileName) throws RemoteException {
        List<ClientInfo> clients = fileToClients.getOrDefault(fileName, new ArrayList<>());
        List<String> clientNames = new ArrayList<>();
        for (ClientInfo client : clients) {
            clientNames.add(client.getClientName());
        }
        return clientNames;
    }

    /**
     * Find all clients daemons addresses who own a specific file.
     */
    @Override
    public List<String> findDaemonAddressesByFile(String fileName) throws RemoteException {
        List<ClientInfo> clients = fileToClients.getOrDefault(fileName, new ArrayList<>());
        List<String> daemonAddresses = new ArrayList<>();
        for (ClientInfo client : clients) {
            daemonAddresses.add(client.getDaemonAdresse());
        }
        return daemonAddresses;
    }
}
