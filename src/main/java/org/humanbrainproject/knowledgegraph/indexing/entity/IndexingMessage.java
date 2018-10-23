package org.humanbrainproject.knowledgegraph.indexing.entity;

/**
 * This is the representation of the indexing message we receive through the API as is (with the payload represented as string).
 */

public class IndexingMessage {
    private final InstanceReference instanceReference;
    private final String payload;

    public IndexingMessage(InstanceReference instanceReference, String payload) {
        this.instanceReference = instanceReference;
        this.payload = payload;
    }

    public InstanceReference getInstanceReference() {
        return instanceReference;
    }

    public String getPayload() {
        return payload;
    }

}
