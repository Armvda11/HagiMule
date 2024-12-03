package hagimule.diary;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
        diary = new DiaryImpl();
    }

    @Test
    void testRegisterAndRetrieveFile() throws RemoteException {
        diary.registerClientFile("Client1", "file1.txt");
        List<String> files = diary.getClientFiles("Client1");
        assertTrue(files.contains("file1.txt"));
    }

    @Test
    void testRemoveFile() throws RemoteException {
        diary.registerClientFile("Client1", "file2.txt");
        String removedFile = diary.removeClientFile("Client1", "file2.txt");
        assertEquals("file2.txt", removedFile);
    }
}
