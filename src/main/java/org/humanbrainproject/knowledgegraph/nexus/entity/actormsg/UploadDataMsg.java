package org.humanbrainproject.knowledgegraph.nexus.entity.actormsg;

import java.util.UUID;

public class UploadDataMsg {
    private UUID id;

    public UploadDataMsg(UUID id){
        this.id = id;
    }

    public UUID getId() {
        return id;
    }
}
