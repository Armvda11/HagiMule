package hagimule.diary;

import static org.junit.jupiter.api.Assertions.*;

import java.rmi.RemoteException;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

// Test class for the DiaryImpl class
class DiaryImplTest {

    // Instance of the DiaryImpl class
    private DiaryImpl diary;

    // Method to set up the test environment before each test
    @BeforeEach
    void setUp() throws RemoteException {
        // Initialize the Diary service before each test
        diary = new DiaryImpl();
    }

    // Test registering and retrieving a file for a client
    @Test
    void testRegisterAndRetrieveFile() throws RemoteException {
        diary.registerClientFile("Client1", "file1.txt");
        List<String> files = diary.getClientFiles("Client1");
        assertTrue(files.contains("file1.txt"), "The file should be registered for the client.");
    }

    // Test removing a file from a client
    @Test
    void testRemoveFile() throws RemoteException {
        diary.registerClientFile("Client1", "file2.txt");
        String removedFile = diary.removeClientFile("Client1", "file2.txt");
        assertEquals("file2.txt", removedFile, "The removed file should match the file registered.");
    }

    // Test attempting to remove a non-existent file
    @Test
    void testRemoveNonExistentFile() throws RemoteException {
        diary.registerClientFile("Client1", "file3.txt");
        Exception exception = assertThrows(RemoteException.class, () -> {
            // Attempting to remove a non-existent file
            diary.removeClientFile("Client1", "nonExistentFile.txt");
        });
        assertEquals("File not found", exception.getMessage(), "The exception message should indicate that the file was not found.");
    }

    // Test registering the same file twice (duplicate registration)
    @Test
    void testRegisterDuplicateFile() throws RemoteException {
        diary.registerClientFile("Client1", "file4.txt");
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            // Trying to register the same file again for the same client
            diary.registerClientFile("Client1", "file4.txt");
        });
        assertEquals("File already registered", exception.getMessage(), "The exception message should indicate that the file is already registered.");
    }

    // Test retrieving files for a client with no files
    @Test
    void testGetFilesForClientWithNoFiles() throws RemoteException {
        List<String> files = diary.getClientFiles("Client2");
        assertTrue(files.isEmpty(), "The client should have no files registered.");
    }

    // Test removing a file from a client with no files
    @Test
    void testRemoveFileFromClientWithNoFiles() throws RemoteException {
        Exception exception = assertThrows(RemoteException.class, () -> {
            // Trying to remove a file from a client that has no files
            diary.removeClientFile("Client2", "nonExistentFile.txt");
        });
        assertEquals("Client not found", exception.getMessage(), "The exception message should indicate that the client does not exist.");
    }

    // Test removing a file from an empty client list
    @Test
    void testRemoveFileFromEmptyClient() throws RemoteException {
        Exception exception = assertThrows(RemoteException.class, () -> {
            // Trying to remove a file from a non-existing client
            diary.removeClientFile("Client3", "file5.txt");
        });
        assertEquals("Client not found", exception.getMessage(), "The exception message should indicate that the client does not exist.");
    }
}
