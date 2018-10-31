package org.humanbrainproject.knowledgegraph.indexing.entity;

import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusInstanceReference;

/**
 * This is the representation of the indexing message we receive through the API as is (with the payload represented as string).
 */

public class IndexingMessage {
    private final NexusInstanceReference instanceReference;
    private final String payload;

    public IndexingMessage(NexusInstanceReference instanceReference, String payload) {
        this.instanceReference = instanceReference;
        this.payload = payload;
    }

    public NexusInstanceReference getInstanceReference() {
        return instanceReference;
    }

    public String getPayload() {
        return payload;
    }

}
