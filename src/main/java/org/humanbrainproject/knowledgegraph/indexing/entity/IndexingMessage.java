package org.humanbrainproject.knowledgegraph.indexing.entity;

import org.humanbrainproject.knowledgegraph.annotations.NoTests;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusInstanceReference;

/**
 * This is the representation of the indexing message we receive through the API as is (with the payload represented as string).
 */
@NoTests(NoTests.NO_LOGIC)
public class IndexingMessage {
    private final NexusInstanceReference instanceReference;
    private final String payload;
    private final String timestamp;
    private final String userId;

    public IndexingMessage(NexusInstanceReference instanceReference, String payload, String timestamp, String userId) {
        this.instanceReference = instanceReference;
        this.payload = payload;
        this.timestamp=timestamp;
        this.userId=userId;
    }

    public NexusInstanceReference getInstanceReference() {
        return instanceReference;
    }

    public String getPayload() {
        return payload;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public String getUserId() {
        return userId;
    }
}
