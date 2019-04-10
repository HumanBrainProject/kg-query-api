package org.humanbrainproject.knowledgegraph.nexus.entity.actormsg;

import java.util.UUID;

public class UploadDoneMsg {
    private UUID id;

    public UploadDoneMsg(UUID id){
        this.id = id;
    }

    public UUID getId() {
        return id;
    }
}
