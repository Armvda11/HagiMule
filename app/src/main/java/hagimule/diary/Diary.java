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
     * Method to register a file for a client
     *  
     * @param clientName client name
     * @param fileName    file name
     * @throws RemoteException if an error occurs during the remote communication
     * @throws IllegalArgumentException if the filename is already registered for the client
     */
    void registerClientFile(String clientName, String fileName) throws RemoteException, IllegalArgumentException;

    /**
     * Method to get the list of files for a client
     * 
     * @param clientName client name
     * @return the list of files owned by the client
     * @throws RemoteException if an error occurs during the remote communication
     */
    List<String> getClientFiles(String clientName) throws RemoteException;

    /**
     * Method to remove a file for a client
     * 
     * @param clientName client name
     * @param fileName    file name
     * @return the file name removed
     * @throws RemoteException if an error occurs during the remote communication
     * @throws IllegalArgumentException if the filename is not registered for the client
     */
    String removeClientFile(String clientName, String fileName) throws RemoteException, IllegalArgumentException;

    /**
     * Method to find all clients who own a specific file
     * 
     * @param fileName the name of the file
     * @return a list of client names who own the file
     * @throws RemoteException if an error occurs during the remote communication
     */
    List<String> findClientsByFile(String fileName) throws RemoteException;
}
