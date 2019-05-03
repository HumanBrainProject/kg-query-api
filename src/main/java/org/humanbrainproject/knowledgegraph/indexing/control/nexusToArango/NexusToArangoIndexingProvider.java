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
        return nativeRepository.findOriginalIdsWithLinkTo(databaseFactory.getDefaultDB(), ArangoDocumentReference.fromNexusInstance(originalId), ArangoCollectionReference.fromFieldName(originalParentProperty));
    }

    public Vertex mapToOriginalSpace(Vertex vertex, NexusInstanceReference originalId) {
        boolean isSuggestion = vertex.getQualifiedIndexingMessage().getOriginalMessage().getInstanceReference().getSubspace().equals(SubSpace.SUGGESTION);
        if(!isSuggestion){
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
        }else {
            return vertex;
        }
    }


    public ArangoConnection getConnection(TargetDatabase database) {
        switch (database) {
            case NATIVE:
                return databaseFactory.getDefaultDB();
            case RELEASE:
                return databaseFactory.getReleasedDB();
            case INFERRED:
                return databaseFactory.getInferredDB();
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
