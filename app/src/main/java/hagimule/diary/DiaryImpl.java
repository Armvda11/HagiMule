package hagimule.diary;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementation of the Diary interface
 * Provides methods to register a file for a client, get the files of a client,
 * remove a file for a client, and find clients by file.
 */
public class DiaryImpl extends UnicastRemoteObject implements Diary {
    
    // Map to store the files of each client: clientName -> List of files
    private Map<String, List<String>> clientFiles = new HashMap<>();
    
    // Map to store the clients who own a specific file: fileName -> List of client names
    private Map<String, List<String>> fileOwners = new HashMap<>();

    // Constructor
    public DiaryImpl() throws RemoteException {
        super();
        this.clientFiles = new HashMap<>();
        this.fileOwners = new HashMap<>();
    }

    /**
     * Registers a file for a specific client.
     * 
     * @param clientName the name of the client
     * @param fileName the name of the file to register
     * @throws RemoteException if a remote exception occurs
     * @throws IllegalArgumentException if the file is already registered for the client
     */
    @Override
    public void registerClientFile(String clientName, String fileName) throws RemoteException, IllegalArgumentException {
        // Get the current list of files for the client
        List<String> files = clientFiles.get(clientName);
        
        // If the client does not have any files yet, create a new list
        if (files == null) {
            files = new ArrayList<>();
            clientFiles.put(clientName, files);
        }
        
        // If the file is already registered, throw an exception
        if (files.contains(fileName)) {
            throw new IllegalArgumentException("File " + fileName + " already registered for client " + clientName);
        }
        
        // Register the file for the client
        files.add(fileName);

        // Update the list of clients owning this file
        fileOwners.computeIfAbsent(fileName, k -> new ArrayList<>()).add(clientName);
    }

    /**
     * Retrieves the list of files for a specific client.
     * 
     * @param clientName the name of the client
     * @return a list of files owned by the client
     * @throws RemoteException if a remote exception occurs
     */
    @Override
    public List<String> getClientFiles(String clientName) throws RemoteException {
        return clientFiles.getOrDefault(clientName, new ArrayList<>());
    }

    /**
     * Removes a file from a client's list of files.
     * 
     * @param clientName the name of the client
     * @param fileName the name of the file to remove
     * @return the name of the removed file
     * @throws RemoteException if a remote exception occurs
     * @throws IllegalArgumentException if the file is not registered for the client
     */
    @Override
    public String removeClientFile(String clientName, String fileName) throws RemoteException, IllegalArgumentException {
        List<String> files = clientFiles.get(clientName);
        
        // If the client does not have any files yet, throw an exception
        if (files == null) {
            throw new IllegalArgumentException("Client " + clientName + " not found");
        }
        
        // If the file is not registered, throw an exception
        if (!files.contains(fileName)) {
            throw new IllegalArgumentException("File " + fileName + " not found for client " + clientName);
        }

        // Remove the file from the client's list
        files.remove(fileName);
        
        // Also remove the client from the list of clients owning the file
        List<String> owners = fileOwners.get(fileName);
        if (owners != null) {
            owners.remove(clientName);
            if (owners.isEmpty()) {
                fileOwners.remove(fileName);  // If no clients own the file, remove it from the map
            }
        }

        return fileName;
    }

    /**
     * Finds all clients who own a specific file.
     * 
     * @param fileName the name of the file
     * @return a list of client names who own the file
     * @throws RemoteException if a remote exception occurs
     */
    public List<String> findClientsByFile(String fileName) throws RemoteException {
        return fileOwners.getOrDefault(fileName, new ArrayList<>());
    }
}
