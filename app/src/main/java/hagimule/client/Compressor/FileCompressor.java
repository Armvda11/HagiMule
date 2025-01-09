package hagimule.client.Compressor;

import java.io.IOException;
import java.nio.file.Path;

public interface FileCompressor {

    /**
     * Compresses a file.
     *
     * @param inputFile Path of the file to compress.
     * @return The path of the compressed file.
     * @throws IOException If an I/O error occurs.
     */
    Path compressFile(Path inputFile) throws IOException;
    Path compressFile(Path inputFile, Path destinationFolderPath) throws IOException;

    /**
     * Decompresses a file.
     *
     * @param compressedFile Path of the compressed file.
     * @return The path of the decompressed file.
     * @throws IOException If an I/O error occurs.
     */
    Path decompressFile(Path compressedFile) throws IOException;
    Path decompressFile(Path compressedFile, Path destinationFolderPath) throws IOException;

    /**
     * Retrieves the file extension.
     *
     * @return the file extension as a String.
     */
    String getExtension();
}
