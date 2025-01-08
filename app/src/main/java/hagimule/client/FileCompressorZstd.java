package hagimule.client;

import com.github.luben.zstd.Zstd;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class FileCompressorZstd implements FileCompressor {

    private static final String COMPRESSED_EXTENSION = ".zst";
    private final int compressionLevel;

    @Override
    public String getExtension() {
        return COMPRESSED_EXTENSION;
    }

    public FileCompressorZstd(int compressionLevel) {
        this.compressionLevel = compressionLevel;
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

        byte[] inputData = Files.readAllBytes(inputFile);
        byte[] compressedData = Zstd.compress(inputData, compressionLevel);
        Files.write(destinationFolderPath, compressedData, StandardOpenOption.CREATE);

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

        byte[] compressedData = Files.readAllBytes(compressedFile);
        long decompressedSize = Zstd.decompressedSize(compressedData);
        byte[] decompressedData = Zstd.decompress(compressedData, (int) decompressedSize);
        Files.write(destinationFolderPath, decompressedData, StandardOpenOption.CREATE);

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
