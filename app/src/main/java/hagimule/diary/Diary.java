package hagimule.diary;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

/**
 * Interface for the Diary (Annuaire in French)
 * Provides methods to register a file for a client, get the files of a client,
 * remove a file for a client, and find clients who own a specific file.
 */
public interface Diary extends Remote {

    /**
     * Register a file for a specific client.
     * @param fileName
     * @param clientName
     * @param daemonAddress
     * @throws RemoteException
     */
    void registerFile(String fileName, String clientName, String daemonAddress) throws RemoteException;


    /**
     * Method to find all clients daemons addresses who own a specific file
     * @param fileName
     * @return
     * @throws RemoteException  
     */
    List<String> findDaemonAddressesByFile(String fileName) throws RemoteException;

    /**
     * Method to find all clients who own a specific file
     * 
     * @param fileName the name of the file
     * @return a list of client names who own the file
     * @throws RemoteException if an error occurs during the remote communication
     */
    List<String> findClientsByFile(String fileName) throws RemoteException;
}
