package org.humanbrainproject.knowledgegraph.nexus.entity.actormsg;

import org.humanbrainproject.knowledgegraph.nexus.entity.FileStructureUploader;

import java.util.UUID;

public class CreateUploadActorMsg {
    private final UUID uuid;
    private FileStructureUploader uploader;

    public CreateUploadActorMsg(UUID uuid, FileStructureUploader uploader){
        this.uuid = uuid;
        this.uploader = uploader;
    }

    public UUID getUuid() {
        return uuid;
    }

    public FileStructureUploader getUploader() {
        return uploader;
    }
}
