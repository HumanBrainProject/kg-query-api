package org.humanbrainproject.knowledgegraph.nexus.entity;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class FileStructureData {

    private final ZipInputStream data;
    private final boolean noDeletion;
    private final Path tmpPath;

    public FileStructureData(ZipInputStream data,  boolean noDeletion){
        this.data = data;
        this.noDeletion = noDeletion;
        UUID generatedId = UUID.randomUUID();
        this.tmpPath = Paths.get(String.format("/tmp/nexusUpload/%s", generatedId.toString()));
    }

    private void storeStructure() throws IOException {
        Files.createDirectories(this.tmpPath);
        ZipEntry entry;
        while ((entry = this.data.getNextEntry()) != null) {
            Path filePath = this.tmpPath.resolve(entry.getName());
            if (!entry.isDirectory()) {
                // if the entry is a file, extracts it
                new File(filePath.toAbsolutePath().toString()).getParentFile().mkdirs();
                extractFile(filePath);
            } else {
                // if the entry is a directory, make the directory
                File dir = new File(filePath.toAbsolutePath().toString());
                dir.mkdirs();
            }


        }
    }
    private void extractFile(Path filePath ) throws IOException{
        byte[] buffer = new byte[2048];
        try (FileOutputStream fos = new FileOutputStream(filePath.toFile());
             BufferedOutputStream bos = new BufferedOutputStream(fos, buffer.length)) {
            int len;
            while ((len = this.data.read(buffer)) > 0) {
                bos.write(buffer, 0, len);
            }
        }
    }

    public File[] listFiles() throws IOException{
        if(!this.tmpPath.toFile().exists()){
            this.storeStructure();
        }
        return this.tmpPath.toFile().listFiles();
    }

    public void cleanData() throws IOException{
        Files.walk(this.tmpPath)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
    }

    public boolean isNoDeletion() {
        return noDeletion;
    }
}