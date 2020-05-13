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

import java.util.Date;

public class UploadStatus {
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

    public int getFailedToUpdated() {
        return failedToUpdate;
    }

    public int getSuccefullyDeleted() {
        return succefullyDeleted;
    }

    public int getSuccefullyCreated() {
        return succefullyCreated;
    }

    public int getSuccefullyUpdated() {
        return succefullyUpdated;
    }

    public int getFailedToDeleted() {
        return failedToDelete;
    }

    public int getFailedToCreated() {
        return failedToCreate;
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

    public int getSchemasFailedToProccess() {
        return schemasFailedToProccess;
    }

    public int getContextFailedToProcess() {
        return contextFailedToProcess;
    }

    public int getSchemasSuccefullyProccessed() {
        return schemasSuccefullyProccessed;
    }

    public int getContextSuccefullyProcessed() {
        return contextSuccefullyProcessed;
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

    private int succefullyDeleted = 0;
    private int succefullyCreated = 0;
    private int succefullyUpdated = 0;
    private int schemasSuccefullyProccessed = 0;
    private int contextSuccefullyProcessed = 0;

    private int failedToDelete = 0;
    private int failedToCreate = 0;
    private int failedToUpdate = 0;
    private int schemasFailedToProccess = 0;
    private int contextFailedToProcess = 0;

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

    public void setCurrentToDelete(int succefullyDeleted, int failedToDeleted){
        this.succefullyDeleted = succefullyDeleted;
        this.failedToDelete = failedToDeleted;
    }

    public void setCurrentToCreate(int succefullyCreated, int failedToCreate){
        this.succefullyCreated = succefullyCreated;
        this.failedToCreate = failedToCreate;
    }

    public void setCurrentToUpdate(int succefullyUpdated, int failedToUpdate){
        this.succefullyUpdated = succefullyUpdated;
        this.failedToUpdate= failedToUpdate;
    }

    public void setSchemasProcessed(int schemasSuccefullyProccessed, int schemasFailedToProccess){
        this.schemasSuccefullyProccessed = schemasSuccefullyProccessed;
        this.schemasFailedToProccess= schemasFailedToProccess;
    }
    public void setContextsProcessed(int contextSuccefullyProcessed, int contextFailedToProcess){
        this.contextSuccefullyProcessed = contextSuccefullyProcessed;
        this.contextFailedToProcess = contextFailedToProcess;
    }

    public void setStatus(Status newStatus){
        this.status = newStatus;
    }

}

