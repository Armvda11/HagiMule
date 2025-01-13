package hagimule.client.Compressor;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileCompressorVide implements FileCompressor {

    private static final String COMPRESSED_EXTENSION = ".vide";

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
        return copyFile(inputFile, compressedFile);
    }

    @Override
    public Path compressFile(Path inputFile, Path destinationFolderPath) throws IOException {
        if (!Files.exists(inputFile)) {
            throw new IllegalArgumentException("Le fichier spécifié n'existe pas : " + inputFile);
        }

        Path compressedFile = destinationFolderPath.resolve(inputFile.getFileName().toString() + COMPRESSED_EXTENSION);
        return copyFile(inputFile, compressedFile);
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
        return copyFile(compressedFile, decompressedFile);
    }

    @Override
    public Path decompressFile(Path compressedFile, Path destinationFolderPath) throws IOException {
        if (!Files.exists(compressedFile)) {
            throw new IllegalArgumentException("Le fichier spécifié n'existe pas : " + compressedFile);
        }

        Path decompressedFile = destinationFolderPath.resolveSibling(removeSuffix(compressedFile.getFileName(), COMPRESSED_EXTENSION).toString());
        return copyFile(compressedFile, decompressedFile);
    }

    private Path addSuffix(Path filePath, String suffix) {
        String fileName = filePath.getFileName().toString();
        return filePath.getParent() != null ? filePath.getParent().resolve(fileName + suffix) : Path.of(fileName + suffix);
    }

    private Path removeSuffix(Path filePath, String suffix) {
        String fileName = filePath.getFileName().toString();
        if (fileName.endsWith(suffix)) {
            fileName = fileName.substring(0, fileName.length() - suffix.length());
        }
        return filePath.getParent() != null ? filePath.getParent().resolve(fileName) : Path.of(fileName);
    }

    private Path copyFile(Path source, Path destination) throws IOException {
        Files.createDirectories(destination.getParent());
        Files.copy(source, destination);
        return destination;
    }
}
