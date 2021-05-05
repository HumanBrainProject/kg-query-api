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
import org.humanbrainproject.knowledgegraph.commons.authorization.control.AuthorizationContext;
import org.humanbrainproject.knowledgegraph.commons.nexus.control.NexusClient;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.ArangoConnection;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.ArangoDatabaseFactory;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.ArangoNativeRepository;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.ArangoRepository;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoCollectionReference;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoDocumentReference;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.entity.Edge;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.entity.SubSpace;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.entity.Vertex;
import org.humanbrainproject.knowledgegraph.indexing.control.MessageProcessor;
import org.humanbrainproject.knowledgegraph.indexing.entity.IndexingMessage;
import org.humanbrainproject.knowledgegraph.indexing.entity.QualifiedIndexingMessage;
import org.humanbrainproject.knowledgegraph.indexing.entity.TargetDatabase;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusInstanceReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@Component
@ToBeTested
public class NexusToArangoIndexingProvider {

    @Autowired
    AuthorizationContext authorizationContext;

    @Autowired
    ArangoDatabaseFactory databaseFactory;

    @Autowired
    ArangoRepository repository;

    @Autowired
    ArangoNativeRepository nativeRepository;

    @Autowired
    NexusClient nexusClient;

    @Autowired
    MessageProcessor messageProcessor;


    public Vertex getVertexStructureById(NexusInstanceReference incomingReference) {
        String payload = getPayloadFromPrimaryStore(incomingReference);
        QualifiedIndexingMessage qualifiedMessage = messageProcessor.qualify(new IndexingMessage(incomingReference, payload, null, null));
        return messageProcessor.createVertexStructure(qualifiedMessage);
    }

    public Set<NexusInstanceReference> findInstancesWithLinkTo(String originalParentProperty, NexusInstanceReference originalId) {
        return nativeRepository.findOriginalIdsWithLinkTo(databaseFactory.getDefaultDB(false), ArangoDocumentReference.fromNexusInstance(originalId), ArangoCollectionReference.fromFieldName(originalParentProperty));
    }

    public Vertex mapToOriginalSpace(Vertex vertex, NexusInstanceReference originalId) {
        QualifiedIndexingMessage newMessage = new QualifiedIndexingMessage(vertex.getQualifiedIndexingMessage().getOriginalMessage(), new LinkedHashMap(vertex.getQualifiedIndexingMessage().getQualifiedMap()));
        Vertex newVertex = messageProcessor.createVertexStructure(newMessage);
        Map<NexusInstanceReference, NexusInstanceReference> toOriginalIdMap = new HashMap<>();
        for (Edge edge : newVertex.getEdges()) {
            NexusInstanceReference relatedOriginalId = nativeRepository.findOriginalId(edge.getReference());
            relatedOriginalId = relatedOriginalId.toSubSpace(SubSpace.MAIN);
            toOriginalIdMap.put(edge.getReference(), relatedOriginalId);
            edge.setReference(relatedOriginalId);
        }
        newVertex.setInstanceReference(originalId);
        newVertex.toSubSpace(SubSpace.MAIN);
        return newVertex;
    }


    public ArangoConnection getConnection(TargetDatabase database) {
        switch (database) {
            case NATIVE:
                return databaseFactory.getDefaultDB(false);
            case RELEASE:
                return databaseFactory.getReleasedDB();
            case INFERRED:
                return databaseFactory.getInferredDB(false);
        }
        return null;
    }


    public String getPayloadFromPrimaryStore(NexusInstanceReference instanceReference) {
        return  nexusClient.get(instanceReference.getRelativeUrl(), authorizationContext.getCredential(), String.class);
    }

    public String getPayloadById(NexusInstanceReference instanceReference, TargetDatabase database) {
        return repository.getPayloadById(ArangoDocumentReference.fromNexusInstance(instanceReference), getConnection(database));
    }

    public NexusInstanceReference findOriginalId(NexusInstanceReference instanceReference){
        return nativeRepository.findOriginalId(instanceReference);
    }
}
