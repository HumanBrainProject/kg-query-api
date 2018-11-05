package org.humanbrainproject.knowledgegraph.indexing.control.nexusToArango;

import org.humanbrainproject.knowledgegraph.commons.nexus.control.SystemNexusClient;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.ArangoConnection;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.ArangoDatabaseFactory;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.ArangoRepository;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoCollectionReference;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoDocumentReference;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.entity.*;
import org.humanbrainproject.knowledgegraph.indexing.control.MessageProcessor;
import org.humanbrainproject.knowledgegraph.indexing.entity.IndexingMessage;
import org.humanbrainproject.knowledgegraph.indexing.entity.QualifiedIndexingMessage;
import org.humanbrainproject.knowledgegraph.indexing.entity.TargetDatabase;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusInstanceReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

@Primary
@Component
public class NexusToArangoIndexingProvider {

    @Autowired
    ArangoDatabaseFactory databaseFactory;

    @Autowired
    ArangoRepository repository;

    @Autowired
    SystemNexusClient systemNexusClient;

    @Autowired
    MessageProcessor messageProcessor;

    public Vertex getVertexStructureById(NexusInstanceReference incomingReference) {
        String payload = systemNexusClient.getPayload(incomingReference);
        QualifiedIndexingMessage qualifiedMessage = messageProcessor.qualify(new IndexingMessage(incomingReference, payload, null, null));
        return messageProcessor.createVertexStructure(qualifiedMessage);
    }

    public Set<NexusInstanceReference> findInstancesWithLinkTo(String originalParentProperty, NexusInstanceReference originalId) {
        return repository.findOriginalIdsWithLinkTo(ArangoDocumentReference.fromNexusInstance(originalId), ArangoCollectionReference.fromFieldName(originalParentProperty), databaseFactory.getDefaultDB());
    }

    public void mapToOriginalSpace(Vertex vertex, NexusInstanceReference originalId) {
        vertex.setInstanceReference(originalId);
        vertex.toSubSpace(SubSpace.MAIN);
        for (EdgeX edge : vertex.getEdges()) {
            NexusInstanceReference relatedOriginalId = repository.findOriginalId(edge.getReference());
            edge.setReference(relatedOriginalId.toSubSpace(SubSpace.MAIN));
        }
    }

    public Set<VertexOrEdgeReference> getVertexOrEdgeReferences(NexusInstanceReference nexusInstanceReference, TargetDatabase database) {
        Set<ArangoDocumentReference> referencesBelongingToInstance = repository.getReferencesBelongingToInstance(nexusInstanceReference, getConnection(database));
        referencesBelongingToInstance.add(ArangoDocumentReference.fromNexusInstance(nexusInstanceReference));
        return referencesBelongingToInstance.stream().map(r -> new VertexOrEdgeReference(){
            @Override
            public String getId() {
                return r.getKey();
            }

            @Override
            public String getTypeName() {
                return r.getCollection().getName();
            }
        }).collect(Collectors.toSet());
    }

    public ArangoConnection getConnection(TargetDatabase database) {
        switch (database) {
            case DEFAULT:
                return databaseFactory.getDefaultDB();
            case RELEASE:
                return databaseFactory.getReleasedDB();
            case INFERRED:
                return databaseFactory.getInferredDB();
        }
        return null;
    }


    public String getPayloadFromPrimaryStore(NexusInstanceReference instanceReference) {
        return systemNexusClient.getPayload(instanceReference);
    }

    public String getPayloadById(NexusInstanceReference instanceReference, TargetDatabase database) {
        return repository.getPayloadById(ArangoDocumentReference.fromNexusInstance(instanceReference), getConnection(database));
    }

    public NexusInstanceReference findOriginalId(NexusInstanceReference instanceReference){
        return repository.findOriginalId(instanceReference);
    }
}
