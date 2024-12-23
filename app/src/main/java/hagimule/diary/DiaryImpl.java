package hagimule.diary;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;

/**
 * Implemantation of the Diary interface
 * this class is used to register and find files for clients
 */
public class DiaryImpl extends UnicastRemoteObject implements Diary {

    // Map to store the clients informations that are connected to the diary, with the client name as key and the client informations as value
    private final Map<String, ClientInfo> Clients = new HashMap<>();
    // Map to store the files of each client , with the file name as key and the set of clients as value
    private final Map<String, Set<ClientInfo>> fileToClients = new HashMap<>();


    // Constructor
    public DiaryImpl() throws RemoteException {
        super();
    }

    /**
     * Send the Client Information or register a new client
     */
    @Override
    public ClientInfo getClient(String clientName, String daemonAddress) throws RemoteException {
        // Si le fichier n'existe pas encore, crée une nouvelle liste de clients pour ce fichier
        Clients.computeIfAbsent(clientName, k -> new ClientInfo(clientName, daemonAddress));
        System.out.println(Clients);
        return Clients.get(clientName);
    }

    /**
     * Register a file for a specific client.
     */
    @Override
    public void registerFile(String fileName, String clientName, String daemonAddress) throws RemoteException {
        // If the file does not exist yet, create a new set of clients for this file
        fileToClients.computeIfAbsent(fileName, k -> new TreeSet<>())
        .add(getClient(clientName, daemonAddress));
        System.out.println( "\n" + fileToClients + "\n");
    }

    /**
     * Find all clients who own a specific file.
     */
    @Override
    public List<String> findClientsByFile(String fileName) throws RemoteException {
        Set<ClientInfo> clients = fileToClients.getOrDefault(fileName, new LinkedHashSet<>());
        List<String> clientNames = new ArrayList<>();
        for (ClientInfo client : clients) {
            clientNames.add(client.getClientName());
        }
        return clientNames;
    }

    /**
     * Find all clients daemons addresses who own a specific file.
     * Return the maxConcurrentDownloads addresses of the least used clients who own the file
     */
    @Override
    public List<String> findDaemonAddressesByFile(String fileName, int maxConcurrentDownloads) throws RemoteException {
        Set<ClientInfo> clients = fileToClients.getOrDefault(fileName, new LinkedHashSet<>());
        List<String> daemonAddresses = new ArrayList<>();

        int i = 0;
        for (ClientInfo client : clients) {
            if (i >= maxConcurrentDownloads) break;
            System.out.println("Ajout du daemon : " + client.getDaemonAdresse() + "sur la taille : " + clients.size());
            daemonAddresses.add(client.getDaemonAdresse());
            i++;
        }
        return daemonAddresses;
    }
}
