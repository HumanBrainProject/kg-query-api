/*
 * Copyright 2018 - 2021 Swiss Federal Institute of Technology Lausanne (EPFL)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * This open source software code was developed in part or in whole in the
 * Human Brain Project, funded from the European Union's Horizon 2020
 * Framework Programme for Research and Innovation under
 * Specific Grant Agreements No. 720270, No. 785907, and No. 945539
 * (Human Brain Project SGA1, SGA2 and SGA3).
 *
 */

package org.humanbrainproject.knowledgegraph.nexus.entity;

import org.slf4j.LoggerFactory;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class FileStructureData {

    private org.slf4j.Logger logger = LoggerFactory.getLogger(FileStructureUploader.class);
    private final ZipInputStream data;
    private final Path tmpPath;
    private final UUID generatedId;

    public FileStructureData(ZipInputStream data){
        this.data = data;
        generatedId = UUID.randomUUID();
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

    public UUID getGeneratedId() {
        return generatedId;
    }

    public void cleanData() throws IOException{
        try {
            Files.walk(this.tmpPath)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        }catch (NoSuchFileException e){
            logger.info("File not found during eviction. It has already been removed");
        }
    }

}
