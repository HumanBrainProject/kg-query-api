/*
 * Copyright 2018 - 2021 Swiss Federal Institute of Technology Lausanne (EPFL)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * This open source software code was developed in part or in whole in the
 * Human Brain Project, funded from the European Union's Horizon 2020
 * Framework Programme for Research and Innovation under
 * Specific Grant Agreements No. 720270, No. 785907, and No. 945539
 * (Human Brain Project SGA1, SGA2 and SGA3).
 *
 */

package org.humanbrainproject.knowledgegraph.indexing.control.nexusToArango;

import org.humanbrainproject.knowledgegraph.annotations.ToBeTested;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.ArangoDatabaseFactory;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.ArangoRepository;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoDocumentReference;
import org.humanbrainproject.knowledgegraph.indexing.entity.QualifiedIndexingMessage;
import org.humanbrainproject.knowledgegraph.query.entity.JsonDocument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Due to the asynchronous nature of data insertion in combination with the immediate indexing mechanism, it is possible
 * that the instance registered in ArangoDB is already newer than the one reported through the indexing API.
 * This logic checks if the message is still relevant or if it can be skipped.
 */
@Component
@ToBeTested(easy = true)
public class RelevanceChecker {

    @Autowired
    ArangoRepository repository;

    @Autowired
    ArangoDatabaseFactory databaseFactory;

    /**
     * @return true if the message should be processed (there is no newer instance in the database) or false if it can be skipped
     */
    public boolean isMessageRelevant(QualifiedIndexingMessage message) {
        Map document = repository.getDocument(ArangoDocumentReference.fromNexusInstance(message.getOriginalMessage().getInstanceReference()), databaseFactory.getDefaultDB(true));
        if (document != null) {
            JsonDocument doc = new JsonDocument(document);
            Integer existingNexusRevision = doc.getNexusRevision();
            String existingNexusId = doc.getNexusId();
            if (message.getOriginalMessage().getInstanceReference().getId() == null || !message.getOriginalMessage().getInstanceReference().getId().equals(existingNexusId)) {
                return true;
            }
            return message.getNexusRevision() == null || existingNexusRevision == null || message.getNexusRevision() >= existingNexusRevision;
        }
        return true;
    }

}
