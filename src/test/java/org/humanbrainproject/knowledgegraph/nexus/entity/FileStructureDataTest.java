package org.humanbrainproject.knowledgegraph.nexus.entity;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.io.*;
import java.nio.file.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class FileStructureDataTest {
    private void createFile(String directory, String fileName) throws IOException{
        Path path = Paths.get(directory);
        Files.createDirectories(path);
        String fileContent = "Hello world";
        Path filePath = Paths.get(directory + "/" +fileName);
        if(!filePath.toFile().exists()){
            Files.createFile(filePath);
        }
        Files.write(filePath, fileContent.getBytes(), StandardOpenOption.TRUNCATE_EXISTING);
    }

    public static void zipFolder(final File folder, final File zipFile) throws IOException {
        zipFolder(folder, new FileOutputStream(zipFile));
    }

    public static void zipFolder(final File folder, final OutputStream outputStream) throws IOException {
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream)) {
            processFolder(folder, zipOutputStream, folder.getPath().length() + 1);
        }
    }

    private static void processFolder(final File folder, final ZipOutputStream zipOutputStream, final int prefixLength)
            throws IOException {
        for (final File file : folder.listFiles()) {
            if (file.isFile()) {
                final ZipEntry zipEntry = new ZipEntry(file.getPath().substring(prefixLength));
                zipOutputStream.putNextEntry(zipEntry);
                try (FileInputStream inputStream = new FileInputStream(file)) {
                    IOUtils.copy(inputStream, zipOutputStream);
                }
                zipOutputStream.closeEntry();
            } else if (file.isDirectory()) {
                processFolder(file, zipOutputStream, prefixLength);
            }
        }
    }
    @Test
    public void listFile() throws IOException{
        String fileName = "f.txt";
        String zipFileName = "f.zip";
        String directory = "/tmp/test";
        String zipDirectory = "/tmp/zip";
        try {
            createFile(directory, fileName);
            Path outputFile = Paths.get(zipDirectory + "/" + zipFileName);
            Files.createDirectories(Paths.get(zipDirectory));
            zipFolder(Paths.get(directory).toFile(), outputFile.toFile());
            FileInputStream fStream = new FileInputStream(outputFile.toFile());
            FileStructureData fs = new FileStructureData(new ZipInputStream(fStream), false);
            File[] files = fs.listFiles();
            assert files.length == 1;
            fs.cleanData();
        } finally {
            Files.deleteIfExists(Paths.get(directory + "/" + fileName));
            Files.deleteIfExists(Paths.get(zipDirectory + "/" + zipFileName));
            Files.deleteIfExists(Paths.get(directory));
            Files.deleteIfExists(Paths.get(zipDirectory));
        }
    }
}
