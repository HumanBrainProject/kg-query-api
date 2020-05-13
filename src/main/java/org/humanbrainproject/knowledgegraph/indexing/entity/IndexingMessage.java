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
