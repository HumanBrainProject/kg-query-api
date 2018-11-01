package org.humanbrainproject.knowledgegraph.indexing.control.nexusToArango;

import org.humanbrainproject.knowledgegraph.commons.jsonld.control.JsonLdToVerticesAndEdges;
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

import java.util.List;
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
    JsonLdToVerticesAndEdges jsonLdToVerticesAndEdges;

    @Autowired
    MessageProcessor messageProcessor;

    public MainVertex getVertexStructureById(NexusInstanceReference incomingReference) {
        String payload = systemNexusClient.getPayload(incomingReference);
        QualifiedIndexingMessage qualifiedMessage = messageProcessor.qualify(new IndexingMessage(incomingReference, payload, null, null));
        ResolvedVertexStructure vertexStructure = messageProcessor.createVertexStructure(qualifiedMessage);
        return vertexStructure.getMainVertex();
    }

    public Set<NexusInstanceReference> findInstancesWithLinkTo(String originalParentProperty, NexusInstanceReference originalId, ReferenceType referenceType) {
        Set<String> originalIdsWithLinkTo = repository.findOriginalIdsWithLinkTo(ArangoDocumentReference.fromNexusInstance(originalId), ArangoCollectionReference.fromFieldName(originalParentProperty, referenceType), databaseFactory.getDefaultDB());
        return originalIdsWithLinkTo.stream().map(NexusInstanceReference::createFromUrl).collect(Collectors.toSet());
    }

    public void mapToOriginalSpace(MainVertex vertex, NexusInstanceReference originalId) {
        NexusInstanceReference instanceReference = vertex.getInstanceReference();
        vertex.setInstanceReference(originalId);
        vertex.toSubSpace(SubSpace.MAIN);
        List<Edge> allEdgesByFollowingEmbedded = vertex.getAllEdgesByFollowingEmbedded();
        allEdgesByFollowingEmbedded.stream().filter(e -> e instanceof InternalEdge).forEach(
                e -> {
                    NexusInstanceReference originalReference = repository.findOriginalId(instanceReference);
                    ((InternalEdge) e).setReference(originalReference);
                }
        );
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
