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

    private int succefullyDeleted = 0;
    private int succefullyCreated = 0;
    private int succefullyUpdated = 0;

    private int failedToDelete = 0;
    private int failedToCreate = 0;
    private int failedToUpdate = 0;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern="yyyy-MM-dd@HH:mm:ss")
    private Date startedAt = null;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern="yyyy-MM-dd@HH:mm:ss")
    private Date finishedAt = null;



    public UploadStatus(boolean isSimulation) {
        this.isSimulation = isSimulation;
        this.status = Status.INITIALIZING;
    }

    public void setInitial(int toDel, int toCreate, int toUpdate) {
        this.numberToCreate = toCreate;
        this.numberToDelete = toDel;
        this.numberToUpdate = toUpdate;
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

    public void setStatus(Status newStatus){
        this.status = newStatus;
    }

}

