package hagimule.client;

import net.jpountz.lz4.LZ4BlockInputStream;
import net.jpountz.lz4.LZ4BlockOutputStream;
import net.jpountz.lz4.LZ4Factory;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class FileCompressorLZ4 implements FileCompressor {

    private static final String COMPRESSED_EXTENSION = ".lz4";

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

        byte[] inputData = Files.readAllBytes(inputFile);
        byte[] compressedData = compressLZ4(inputData);
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
        byte[] decompressedData = decompressLZ4(compressedData);
        Files.write(destinationFolderPath, decompressedData, StandardOpenOption.CREATE);

        return destinationFolderPath;
    }

    private byte[] compressLZ4(byte[] inputData) throws IOException {
        try (OutputStream out = Files.newOutputStream(Path.of("compressed.lz4"));
             LZ4BlockOutputStream lz4Out = new LZ4BlockOutputStream(out, 64 * 1024)) { // Taille du buffer
            lz4Out.write(inputData);
        }
        return Files.readAllBytes(Path.of("compressed.lz4"));
    }

    private byte[] decompressLZ4(byte[] compressedData) throws IOException {
        LZ4Factory factory = LZ4Factory.fastestInstance();
        try (InputStream in = Files.newInputStream(Path.of("compressed.lz4"));
            LZ4BlockInputStream lz4In = new LZ4BlockInputStream(in, factory.fastDecompressor())) {
            return lz4In.readAllBytes();
        }
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
