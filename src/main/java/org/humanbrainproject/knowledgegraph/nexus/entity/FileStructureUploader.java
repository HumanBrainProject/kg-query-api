package org.humanbrainproject.knowledgegraph.nexus.entity;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.humanbrainproject.knowledgegraph.commons.authorization.control.AuthorizationContext;
import org.humanbrainproject.knowledgegraph.commons.authorization.control.DefaultAuthorizationContext;
import org.humanbrainproject.knowledgegraph.commons.authorization.entity.Credential;
import org.humanbrainproject.knowledgegraph.commons.nexus.control.NexusClient;
import org.humanbrainproject.knowledgegraph.commons.nexus.control.NexusConfiguration;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusRelativeUrl;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusSchemaReference;
import org.humanbrainproject.knowledgegraph.instances.control.SchemaController;
import org.humanbrainproject.knowledgegraph.query.entity.JsonDocument;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class FileStructureUploader {

    private final UploadStatus status;
    private NexusDataStructure data;
    private NexusClient nexusClient;
    private SchemaController schemaController;

    private Credential credential;
    private boolean noDeletion;
    private FileStructureDataExtractor fileStructureDataExtractor;
    private final Integer MAX_TRIES = 5;

    private org.slf4j.Logger logger = LoggerFactory.getLogger(FileStructureUploader.class);

    public FileStructureUploader(NexusDataStructure data, SchemaController schemaController, NexusClient nexusClient,
                                 Credential credential,FileStructureDataExtractor fileStructureDataExtractor, boolean noDeletion ){
        this.data = data;
        this.schemaController = schemaController;
        this.nexusClient = nexusClient;
        this.credential = credential;
        this.noDeletion = noDeletion;
        this.fileStructureDataExtractor = fileStructureDataExtractor;
        this.status = new UploadStatus();
    }

    public void uploadData() throws IOException, InterruptedException {
        this.status.setInitial(this.data.getToDelete().size(), this.data.getToCreate().entrySet()
                .stream().mapToInt(entry -> entry.getValue().size()).sum(), this.data.getToUpdate().size());
        for(NexusSchemaReference s: this.data.getSchemasConcerned()){
            this.schemaController.createSchema(s, this.schemaController.createSimpleSchema(s), this.credential);
        }
        this.status.setStatus(UploadStatus.Status.PROCESSING);
        if(!this.noDeletion) {
            this.withRetry(0, this.data.getToDelete(), this::executeDelete, true);
        }
        this.withRetry(0, this.data.getToUpdate(), this::executeUpdate, true);
        this.withRetry(0, this.data.getToCreate(), this::executeCreate, true);
        this.status.setStatus(UploadStatus.Status.DONE);
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
            boolean result = this.nexusClient.delete(url, rev,this.credential);
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
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> json = mapper.readValue(el.getValue(), Map.class);
            JsonDocument res = this.nexusClient.put(url, rev, json, this.credential);
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
                ObjectMapper mapper = new ObjectMapper();
                Map<String, Object> json = mapper.readValue(f, Map.class);
                JsonDocument res = this.nexusClient.post(url, null, json, this.credential);
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


}
