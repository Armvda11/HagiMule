package hagimule.diary;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

/**
 * Interface for the Diary (Annuaire in French)
 * Provides methods to register a file for a client and to get the files of a client
 */
public interface Diary extends Remote{

    /**
     * Method to register a file for a client
     *  
     * @param clientName client name
     * @param fileName    file name
     * @throws RemoteException if an error occurs during the remote communication
     * @throws IllegalArgumentException if the filename is already registered
     */
    void registerClientFile(String clientName, String fileName) throws RemoteException , IllegalArgumentException;

    /**
     * Method to get the files of a client
     * @param clientName client name
     * @return the list of files of the client
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
     * @throws IllegalArgumentException if the filename is not registered
     */
    String removeClientFile(String clientName, String fileName) throws RemoteException, IllegalArgumentException;

}