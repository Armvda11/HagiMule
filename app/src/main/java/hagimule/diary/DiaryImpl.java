package hagimule.diary;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementation of the Diary interface
 * Provides methods to register a file for a client and to get the files of a client
 */
public class DiaryImpl extends UnicastRemoteObject implements Diary {
    // Map to store the files of each client
    private Map<String, List<String>> clientFiles = new HashMap<>();

    // Constructor
    public DiaryImpl() throws RemoteException {
        super();
        this.clientFiles = new HashMap<>();
    }


    // Implementation of the registerClientFile method
    // !!! im not sure if the client could have multiple files
    @Override
    public void registerClientFile(String clientName, String fileName) throws RemoteException, IllegalArgumentException {
        List<String> files = clientFiles.get(clientName);
        // If the client does not have any files yet, create a new list
        if (files == null) {
            files = new ArrayList<>();
            clientFiles.put(clientName, files);
        }
        // If the file is already registered, throw an exception
        if (files.contains(fileName)) {
            throw new IllegalArgumentException("File "+ fileName + " already registered for client "+ clientName);
        }
        files.add(fileName);
    }

    // implementation of the getClientFiles method
    @Override
    public List<String> getClientFiles(String clientName) throws RemoteException{
        return clientFiles.get(clientName);
    }


    // Implementation of the removeClientFile method
    @Override
    public String removeClientFile(String clientName, String fileName) throws RemoteException, IllegalArgumentException {
        List<String> files = clientFiles.get(clientName);
        // If the client does not have any files yet, throw an exception
        if (files == null) {
            throw new IllegalArgumentException("Client"+clientName+ "not found");
        }

        // if the file is not registered, throw an exception
        if (!files.contains(fileName)) {
            throw new IllegalArgumentException("File "+ fileName + " not found for client "+ clientName);
        }
        files.remove(fileName);
        return fileName;
    }
}