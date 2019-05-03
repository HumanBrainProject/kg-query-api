package org.humanbrainproject.knowledgegraph.nexus.entity.actormsg;

import java.util.UUID;

public class GetStatusMsg {
    private UUID uuid;

    public GetStatusMsg(UUID uuid){

        this.uuid = uuid;
    }

    public UUID getUuid() {
        return uuid;
    }
}
