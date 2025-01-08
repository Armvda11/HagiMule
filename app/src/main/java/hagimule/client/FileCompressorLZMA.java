package hagimule.client;

import org.tukaani.xz.LZMA2Options;
import org.tukaani.xz.XZInputStream;
import org.tukaani.xz.XZOutputStream;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileCompressorLZMA implements FileCompressor {

    private static final String COMPRESSED_EXTENSION = ".lzma";

    @Override
    public String getExtension() {
        return COMPRESSED_EXTENSION;
    }

    @Override
    public Path compressFile(Path inputFile) throws IOException {
        if (!Files.exists(inputFile)) {
            throw new IllegalArgumentException("Le fichier spécifié n'existe pas : " + inputFile);
        }

        Path compressedFile = addSuffix(inputFile, COMPRESSED_EXTENSION);
        return compressFile(inputFile, compressedFile);
    }

    @Override
    public Path compressFile(Path inputFile, Path destinationFolderPath) throws IOException {
        if (!Files.exists(inputFile)) {
            throw new IllegalArgumentException("Le fichier spécifié n'existe pas : " + inputFile);
        }

        try (InputStream inputStream = Files.newInputStream(inputFile);
            OutputStream outputStream = new BufferedOutputStream(
                new XZOutputStream(Files.newOutputStream(destinationFolderPath), new LZMA2Options())
                )) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        }

        return destinationFolderPath;
    }

    @Override
    public Path decompressFile(Path compressedFile) throws IOException {
        if (!Files.exists(compressedFile)) {
            throw new IllegalArgumentException("Le fichier spécifié n'existe pas : " + compressedFile);
        }

        if (!compressedFile.toString().endsWith(COMPRESSED_EXTENSION)) {
            throw new IllegalArgumentException("Le fichier n'a pas l'extension attendue : " + COMPRESSED_EXTENSION);
        }

        Path decompressedFile = removeSuffix(compressedFile, COMPRESSED_EXTENSION);
        return decompressFile(compressedFile, decompressedFile);
    }

    @Override
    public Path decompressFile(Path compressedFile, Path destinationFolderPath) throws IOException {
        if (!Files.exists(compressedFile)) {
            throw new IllegalArgumentException("Le fichier spécifié n'existe pas : " + compressedFile);
        }

        try (InputStream inputStream = new XZInputStream(Files.newInputStream(compressedFile));
            OutputStream outputStream = Files.newOutputStream(destinationFolderPath)) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        }

        return destinationFolderPath;
    }
    
    private Path addSuffix(Path filePath, String suffix) {
        String fileName = filePath.getFileName().toString();
        return filePath.getParent().resolve(fileName + suffix);
    }

    private Path removeSuffix(Path filePath, String suffix) {
        String fileName = filePath.getFileName().toString();
        if (fileName.endsWith(suffix)) {
            fileName = fileName.substring(0, fileName.length() - suffix.length());
        }
        return filePath.getParent().resolve(fileName);
    }
}
