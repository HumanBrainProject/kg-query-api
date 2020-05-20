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

import com.fasterxml.jackson.annotation.JsonFormat;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusRelativeUrl;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusSchemaReference;

import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class UploadStatus {
    private ErrorsAndSuccess<Map<NexusSchemaReference, List<File>>, Map<NexusRelativeUrl, Map<String, Object>>> errorsAndSuccessOnCreation;
    private ErrorsAndSuccess<Map<String, File>, Map<NexusRelativeUrl, Map<String, Object>>> errorsAndSuccessOnUpdate;
    private ErrorsAndSuccess<Set<String>, Set<NexusRelativeUrl>> errorsAndSuccessOnDelete;
    private ErrorsAndSuccess<Map<NexusSchemaReference, File>, Integer> errorsAndSuccessOnSchemaProcessing;
    private ErrorsAndSuccess<Map<NexusSchemaReference, File>, Integer> errorsAndSuccessOnContextProcessing;

    public Status getStatus() {
        return status;
    }

    public int getNumberToDelete() {
        return numberToDelete;
    }

    public int getNumberToCreate() {
        return numberToCreate;
    }

    public int getNumberToUpdate() {
        return numberToUpdate;
    }

    public Set<String> getFailedToUpdated() {
        return errorsAndSuccessOnUpdate.errors.keySet();
    }

    public Set<NexusRelativeUrl> getSuccefullyDeleted() {
        return errorsAndSuccessOnDelete.success;
    }

    public  Map<NexusRelativeUrl, Map<String, Object>> getSuccefullyCreated() {
        return errorsAndSuccessOnCreation.success;
    }

    public Map<NexusRelativeUrl, Map<String, Object>> getSuccefullyUpdated() {
        return errorsAndSuccessOnUpdate.success;
    }

    public Set<String> getFailedToDeleted() {
        return errorsAndSuccessOnDelete.errors;
    }

    public Set<NexusSchemaReference> getFailedToCreate() {
        return errorsAndSuccessOnCreation.errors.keySet();
    }

    public boolean isSimulation() {
        return isSimulation;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setFinishedAt() {
        this.finishedAt = new Date();
    }

    public Set<NexusSchemaReference> getSchemasFailedToProccess() {
        return errorsAndSuccessOnSchemaProcessing.errors.keySet();
    }

    public Set<NexusSchemaReference> getContextFailedToProcess() {
        return errorsAndSuccessOnContextProcessing.errors.keySet();
    }

    public int getSchemasSuccefullyProccessed() {
        return errorsAndSuccessOnSchemaProcessing.success;
    }

    public int getContextSuccefullyProcessed() {
        return errorsAndSuccessOnContextProcessing.success;
    }

    public int getSchemaFilesFound() {
        return schemaFilesFound;
    }

    public int getContextFilesFound() {
        return contextFilesFound;
    }

    enum Status {
        INITIALIZING,
        PROCESSING,
        DONE,
        FAILED
    }

    private Status status;
    private boolean isSimulation;
    private String message = "";

    private int numberToDelete = 0;
    private int numberToCreate = 0;
    private int numberToUpdate = 0;
    private int schemaFilesFound = 0;
    private int contextFilesFound = 0;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern="yyyy-MM-dd@HH:mm:ss")
    private Date startedAt = null;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern="yyyy-MM-dd@HH:mm:ss")
    private Date finishedAt = null;



    public UploadStatus(boolean isSimulation) {
        this.isSimulation = isSimulation;
        this.status = Status.INITIALIZING;
    }

    public void setInitial(int toDel, int toCreate, int toUpdate, int schemaFilesFound, int contextFilesFound) {
        this.numberToCreate = toCreate;
        this.numberToDelete = toDel;
        this.numberToUpdate = toUpdate;
        this.schemaFilesFound = schemaFilesFound;
        this.contextFilesFound = contextFilesFound;
        this.startedAt = new Date();
    }

    public void setCurrentToDelete(ErrorsAndSuccess<Set<String>, Set<NexusRelativeUrl>> errorsAndSuccessOnDelete){
        this.errorsAndSuccessOnDelete = errorsAndSuccessOnDelete;
    }

    public void setCurrentToCreate(ErrorsAndSuccess<Map<NexusSchemaReference, List<File>>, Map<NexusRelativeUrl, Map<String, Object>>> errorsAndSuccessOnCreation){
        this.errorsAndSuccessOnCreation = errorsAndSuccessOnCreation;
    }

    public void setCurrentToUpdate(ErrorsAndSuccess<Map<String, File>, Map<NexusRelativeUrl, Map<String, Object>>> errorsAndSuccessOnUpdate){
        this.errorsAndSuccessOnUpdate = errorsAndSuccessOnUpdate;
    }

    public void setSchemasProcessed(ErrorsAndSuccess<Map<NexusSchemaReference,File>, Integer> errorsAndSuccessOnSchemaProcessing){
        this.errorsAndSuccessOnSchemaProcessing = errorsAndSuccessOnSchemaProcessing;
    }
    public void setContextsProcessed(ErrorsAndSuccess<Map<NexusSchemaReference,File>, Integer> errorsAndSuccessOnContextProcessing){
        this.errorsAndSuccessOnContextProcessing = errorsAndSuccessOnContextProcessing;
    }

    public void setStatus(Status newStatus){
        this.status = newStatus;
    }

}

