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
import org.springframework.web.client.HttpClientErrorException;

import java.io.File;
import java.io.IOException;
import java.util.*;

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
        R apply(T t, int f) throws IOException;
    }

    protected <T> T withRetry(Integer tries, T listToExecute, CheckedFunction<T, T> execute, boolean hasRetryDelay)  throws IOException, InterruptedException{
        boolean shouldContinue = true;
        T errors = null;
        while(tries < this.MAX_TRIES && shouldContinue) {
            errors = execute.apply(listToExecute, 0);
            if(errors == null){
                shouldContinue = false;
            }else{
                listToExecute = errors;
                tries += 1;
            }
            if(hasRetryDelay){
                logger.debug("Retrying in " + 6 * tries + " seconds");
                Thread.sleep(6000 * tries);
            }
        }
        return errors;
    }

    protected final List<String> executeDelete(List<String> toDelete, int successfullyProcessed){
        List<String> errors = new ArrayList<>();
        for(String s: toDelete){
            NexusRelativeUrl url = new NexusRelativeUrl(NexusConfiguration.ResourceType.DATA, s);
            JsonDocument doc = this.nexusClient.get(url, this.credential);
            Integer rev = doc.getNexusRevision();
            boolean result = false;
            if(!this.status.isSimulation()){
                result = this.nexusClient.delete(url, rev,this.credential);
            }else{
                result = true;
            }
            if(!result){
                errors.add(s);
            }else{
                successfullyProcessed += 1;
            }
            this.status.setCurrentToDelete(successfullyProcessed, errors.size());
        }
        if(errors.isEmpty()){
            return null;
        }else{
            return errors;
        }
    }

    protected final Map<String, File> executeUpdate(Map <String, File> toUpdate, int successfullyProcessed) throws IOException {
        Map<String, File> errors = new HashMap<>();
        for(Map.Entry<String, File> el: toUpdate.entrySet()){
            NexusRelativeUrl url = new NexusRelativeUrl(NexusConfiguration.ResourceType.DATA, el.getKey());
            JsonDocument doc = this.nexusClient.get(url, this.credential);
            Integer rev = doc.getNexusRevision();
            Map<String, Object> json = this.mapper.readValue(el.getValue(), Map.class);
            JsonDocument res = null;
            if(!this.status.isSimulation()){
                try{
                    res = this.nexusClient.put(url, rev, json, this.credential);
                } catch (HttpClientErrorException e){
                    res = null;
                }
            }else{
                res = new JsonDocument();
            }
            if(res == null){
                errors.put(el.getKey(), el.getValue());
            }else{
                successfullyProcessed += 1;
            }
            this.status.setCurrentToUpdate(successfullyProcessed, errors.size());
        }
        if(errors.isEmpty()){
            return null;
        }else{
            return errors;
        }
    }

    protected final Map<NexusSchemaReference, List<File>> executeCreate(Map<NexusSchemaReference, List<File>> toCreate, int successfullyProcessed) throws IOException{
        Map<NexusSchemaReference, List<File>> errors = new HashMap<>();
        for(Map.Entry<NexusSchemaReference, List<File>> el: toCreate.entrySet()) {
            List<File> fileErrors = new ArrayList<>();
            for (File f : el.getValue()) {
                NexusRelativeUrl url = new NexusRelativeUrl(NexusConfiguration.ResourceType.DATA, el.getKey().toString());
                Map<String, Object> json = this.mapper.readValue(f, Map.class);
                JsonDocument res = null;
                if(!this.status.isSimulation()) {
                    try{
                        res = this.nexusClient.post(url, null, json, this.credential);
                    } catch (HttpClientErrorException e){
                        res = null;
                    }
                }else{
                    res = new JsonDocument();
                }
                if (res == null) {
                    fileErrors.add(f);
                } else {
                    successfullyProcessed += 1;
                }
                this.status.setCurrentToCreate(successfullyProcessed, errors.size());
            }
            if (!fileErrors.isEmpty()) {
                errors.put(el.getKey(), fileErrors);
            }
        }
        if(errors.isEmpty()){
            return null;
        }else{
            return errors;
        }
    }

    protected final Map<NexusSchemaReference,File> executeSchemaCreate(Map<NexusSchemaReference, File> schemasToCreate, int successfullyProcessed) throws IOException{
        Map<NexusSchemaReference, File> errors = new HashMap<>();
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
                successfullyProcessed += 1;
            } catch (HttpClientErrorException e){
                errors.put(s.getKey(), s.getValue());
            }
        }
        this.status.setSchemasProcessed(successfullyProcessed, errors.size());
        if(errors.isEmpty()){
            return null;
        }else{
            return errors;
        }

    }

    protected final Map<NexusSchemaReference,File> executeContextCreate(Map<NexusSchemaReference, File> contextToCreate, int successfullyProcessed) throws IOException{
        Map<NexusSchemaReference, File> errors = new HashMap<>();
        for(Map.Entry<NexusSchemaReference, File> contextEntry: contextToCreate.entrySet()){
            Map<String, Object> json = this.mapper.readValue(contextEntry.getValue(), Map.class);
            try{
                if(!this.status.isSimulation()) {
                    this.contextController.createContext(contextEntry.getKey(), json, this.credential);
                }
                successfullyProcessed += 1;
            }catch (HttpClientErrorException e){
                errors.put(contextEntry.getKey(), contextEntry.getValue());
            }
            this.status.setContextsProcessed(successfullyProcessed, errors.size());
        }
        if(errors.isEmpty()){
            return null;
        }else{
            return errors;
        }
    }


}
