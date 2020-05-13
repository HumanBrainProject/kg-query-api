/*
 * Copyright 2020 EPFL/Human Brain Project PCO
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.humanbrainproject.knowledgegraph.nexus.entity;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.humanbrainproject.knowledgegraph.commons.authorization.entity.Credential;
import org.humanbrainproject.knowledgegraph.commons.nexus.control.NexusClient;
import org.humanbrainproject.knowledgegraph.commons.nexus.control.NexusConfiguration;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusRelativeUrl;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusSchemaReference;
import org.humanbrainproject.knowledgegraph.instances.control.ContextController;
import org.humanbrainproject.knowledgegraph.instances.control.SchemaController;
import org.humanbrainproject.knowledgegraph.query.entity.JsonDocument;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;

class ErrorsAndSuccess<T> {
    T errors;
    int success = 0;
}

public class FileStructureUploader {

    private final UploadStatus status;
    private NexusDataStructure data;
    private NexusClient nexusClient;
    private SchemaController schemaController;
    private ContextController contextController;

    private Credential credential;
    private boolean noDeletion;
    private FileStructureDataExtractor fileStructureDataExtractor;
    private final Integer MAX_TRIES = 5;
    private final ObjectMapper mapper = new ObjectMapper();

    private org.slf4j.Logger logger = LoggerFactory.getLogger(FileStructureUploader.class);



    public FileStructureUploader(NexusDataStructure data, SchemaController schemaController, ContextController contextController, NexusClient nexusClient,
                                 Credential credential,FileStructureDataExtractor fileStructureDataExtractor,
                                 boolean noDeletion, boolean isSimulation){
        this.data = data;
        this.schemaController = schemaController;
        this.contextController = contextController;
        this.nexusClient = nexusClient;
        this.credential = credential;
        this.noDeletion = noDeletion;
        this.fileStructureDataExtractor = fileStructureDataExtractor;
        this.status = new UploadStatus(isSimulation);
    }

    public void uploadData() throws IOException {
        try {
            this.status.setInitial(
                    this.data.getToDelete().size(),
                    this.data.getToCreate().values().stream().mapToInt(List<File>::size).sum(),
                    this.data.getToUpdate().size(),
                    this.data.getSchemasConcerned().size(),
                    this.data.getContextFiles().size()
                    );
            this.status.setStatus(UploadStatus.Status.PROCESSING);
            this.withRetry(0, this.data.getContextFiles(), this::executeContextCreate, true);
            this.withRetry(0, this.data.getSchemasConcerned(), this::executeSchemaCreate, true);
            if(!this.noDeletion) {
                this.withRetry(0, this.data.getToDelete(), this::executeDelete, true);
            }
            this.withRetry(0, this.data.getToUpdate(), this::executeUpdate, true);
            this.withRetry(0, this.data.getToCreate(), this::executeCreate, true);
            if(this.status.isSimulation()){
                this.status.setMessage("Simulation successful");
            }
            this.status.setStatus(UploadStatus.Status.DONE);
            this.status.setFinishedAt();

        } catch (Exception e){
            this.status.setStatus(UploadStatus.Status.FAILED);
            logger.error(e.getMessage());
            this.status.setMessage(e.getMessage());
            this.fileStructureDataExtractor.cleanData();
        }
    }

    public UploadStatus getStatus(){
        return this.status;
    }

    public FileStructureDataExtractor getFileStructureDataExtractor() {
        return fileStructureDataExtractor;
    }

    @FunctionalInterface
    public interface CheckedFunction<T, R> {
        R apply(T t, ErrorsAndSuccess<T> f);
    }

    protected <T> ErrorsAndSuccess<T> withRetry(Integer tries, T listToExecute, CheckedFunction<T, ErrorsAndSuccess<T>> execute, boolean hasRetryDelay) throws InterruptedException{
        boolean shouldContinue = true;
        ErrorsAndSuccess<T> errors = new ErrorsAndSuccess<>();
        while(tries < this.MAX_TRIES && shouldContinue) {
            errors = execute.apply(listToExecute, errors);
            if(errors.errors == null){
                shouldContinue = false;
            }else{
                listToExecute = errors.errors;
                tries += 1;
            }
            if(hasRetryDelay){
                logger.debug("Retrying in " + 6 * tries + " seconds");
                Thread.sleep(6000 * tries);
            }
        }
        return errors;
    }

    protected final ErrorsAndSuccess<List<String>> executeDelete(List<String> toDelete,  ErrorsAndSuccess<List<String>> errorsAndSuccess ){
        errorsAndSuccess.errors = new ArrayList<>();
        for(String s: toDelete){
            boolean result = false;
            if(!this.status.isSimulation()){
                try {
                    NexusRelativeUrl url = new NexusRelativeUrl(NexusConfiguration.ResourceType.DATA, s);
                    JsonDocument doc = this.nexusClient.get(url, this.credential);
                    Integer rev = doc.getNexusRevision();
                    result = this.nexusClient.delete(url, rev,this.credential);
                }catch (Exception e){
                    e.printStackTrace();
                    result = false;
                }
            }else{
                result = true;
            }
            if(!result){
                errorsAndSuccess.errors.add(s);
            }else{
                errorsAndSuccess.success += 1;
            }
            this.status.setCurrentToDelete(errorsAndSuccess.success, errorsAndSuccess.errors.size());
        }
        if(errorsAndSuccess.errors.isEmpty()){
            errorsAndSuccess.errors = null;
        }
        return errorsAndSuccess;
    }

    protected final ErrorsAndSuccess<Map<String, File>> executeUpdate(Map <String, File> toUpdate, ErrorsAndSuccess<Map<String,File>> errorsAndSuccess) {
        errorsAndSuccess.errors = new HashMap<>();
        for(Map.Entry<String, File> el: toUpdate.entrySet()){
            JsonDocument res = null;
            if(!this.status.isSimulation()){
                try{
                    NexusRelativeUrl url = new NexusRelativeUrl(NexusConfiguration.ResourceType.DATA, el.getKey());
                    JsonDocument doc = this.nexusClient.get(url, this.credential);
                    Integer rev = doc.getNexusRevision();
                    Map<String, Object> json = this.mapper.readValue(el.getValue(), Map.class);
                    res = this.nexusClient.put(url, rev, json, this.credential);
                } catch (Exception e){
                    res = null;
                }
            }else{
                res = new JsonDocument();
            }
            if(res == null){
                errorsAndSuccess.errors.put(el.getKey(), el.getValue());
            }else{
                errorsAndSuccess.success += 1;
            }
            this.status.setCurrentToUpdate(errorsAndSuccess.success, errorsAndSuccess.errors.size());
        }
        if(errorsAndSuccess.errors.isEmpty()){
            errorsAndSuccess.errors = null;
        }
        return errorsAndSuccess;
    }

    protected final ErrorsAndSuccess<Map<NexusSchemaReference, List<File>>> executeCreate(Map<NexusSchemaReference, List<File>> toCreate, ErrorsAndSuccess< Map<NexusSchemaReference, List<File>>> errorsAndSuccess){
        errorsAndSuccess.errors = new HashMap<>();
        for(Map.Entry<NexusSchemaReference, List<File>> el: toCreate.entrySet()) {
            List<File> fileErrors = new ArrayList<>();
            for (File f : el.getValue()) {
                JsonDocument res = null;
                if(!this.status.isSimulation()) {
                    try{
                        NexusRelativeUrl url = new NexusRelativeUrl(NexusConfiguration.ResourceType.DATA, el.getKey().toString());
                        Map<String, Object> json = this.mapper.readValue(f, Map.class);
                        res = this.nexusClient.post(url, null, json, this.credential);
                    } catch (Exception e){
                        res = null;
                    }
                }else{
                    res = new JsonDocument();
                }
                if (res == null) {
                    fileErrors.add(f);
                } else {
                    errorsAndSuccess.success += 1;
                }
                this.status.setCurrentToCreate( errorsAndSuccess.success,  errorsAndSuccess.errors.size());
            }
            if (!fileErrors.isEmpty()) {
                errorsAndSuccess.errors.put(el.getKey(), fileErrors);
            }
        }
        if( errorsAndSuccess.errors.isEmpty()){
            errorsAndSuccess.errors = null;
        }
        return errorsAndSuccess;
    }

    protected final ErrorsAndSuccess<Map<NexusSchemaReference,File>> executeSchemaCreate(Map<NexusSchemaReference, File> schemasToCreate, ErrorsAndSuccess<Map<NexusSchemaReference,File>> errorsAndSuccess) {
        errorsAndSuccess.errors = new HashMap<>();
        for(Map.Entry<NexusSchemaReference, File> s: schemasToCreate.entrySet()){
            try{
                if(!this.status.isSimulation()) {
                    if (s.getValue() == null) {
                        this.schemaController.createSchema(s.getKey(), this.schemaController.createSimpleSchema(s.getKey()), this.credential);
                    } else {
                        Map<String, Object> json = this.mapper.readValue(s.getValue(), Map.class);
                        this.schemaController.createSchema(s.getKey(), json, this.credential);
                    }
                }
                errorsAndSuccess.success += 1;
            } catch (Exception e){
                errorsAndSuccess.errors.put(s.getKey(), s.getValue());
            }
        }
        this.status.setSchemasProcessed(errorsAndSuccess.success, errorsAndSuccess.errors.size());
        if(errorsAndSuccess.errors.isEmpty()) {
            errorsAndSuccess.errors = null;
        }
        return errorsAndSuccess;

    }

    protected final  ErrorsAndSuccess<Map<NexusSchemaReference,File>> executeContextCreate(Map<NexusSchemaReference, File> contextToCreate,  ErrorsAndSuccess<Map<NexusSchemaReference,File>> errorsAndSuccess)
    {
        errorsAndSuccess.errors = new HashMap<>();
        for(Map.Entry<NexusSchemaReference, File> contextEntry: contextToCreate.entrySet()){
            try{
                Map<String, Object> json = this.mapper.readValue(contextEntry.getValue(), Map.class);
                if(!this.status.isSimulation()) {
                    this.contextController.createContext(contextEntry.getKey(), json, this.credential);
                }
                errorsAndSuccess.success += 1;
            }catch (Exception e){
                errorsAndSuccess.errors.put(contextEntry.getKey(), contextEntry.getValue());
            }
            this.status.setContextsProcessed(errorsAndSuccess.success, errorsAndSuccess.errors.size());
        }
        if(errorsAndSuccess.errors.isEmpty()){
            errorsAndSuccess.errors = null;
        }
        return errorsAndSuccess;

    }


}
