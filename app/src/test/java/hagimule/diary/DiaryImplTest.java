package hagimule.diary;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.rmi.RemoteException;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

class DiaryImplTest {

    private DiaryImpl diary;

    @Before
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
