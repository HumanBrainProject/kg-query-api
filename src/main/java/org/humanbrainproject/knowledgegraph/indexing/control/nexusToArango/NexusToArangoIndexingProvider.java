package org.humanbrainproject.knowledgegraph.indexing.control.nexusToArango;

import org.humanbrainproject.knowledgegraph.commons.jsonld.control.JsonLdToVerticesAndEdges;
import org.humanbrainproject.knowledgegraph.commons.nexus.control.SystemNexusClient;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.ArangoConnection;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.ArangoDatabaseFactory;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.ArangoRepository;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoCollectionReference;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoDocumentReference;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.entity.*;
import org.humanbrainproject.knowledgegraph.indexing.control.IndexingProvider;
import org.humanbrainproject.knowledgegraph.indexing.control.MessageProcessor;
import org.humanbrainproject.knowledgegraph.indexing.entity.IndexingMessage;
import org.humanbrainproject.knowledgegraph.indexing.entity.InstanceReference;
import org.humanbrainproject.knowledgegraph.indexing.entity.QualifiedIndexingMessage;
import org.humanbrainproject.knowledgegraph.indexing.entity.TargetDatabase;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusInstanceReference;
import org.humanbrainproject.knowledgegraph.indexing.exception.WrongReferenceTypeException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Primary
@Component
public class NexusToArangoIndexingProvider implements IndexingProvider {

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

    @Override
    public MainVertex getVertexStructureById(InstanceReference incomingReference) {
        String payload = systemNexusClient.getPayload(castReference(incomingReference));
        QualifiedIndexingMessage qualifiedMessage = messageProcessor.qualify(new IndexingMessage(incomingReference, payload));
        ResolvedVertexStructure vertexStructure = messageProcessor.createVertexStructure(qualifiedMessage);
        return vertexStructure.getMainVertex();
    }

    @Override
    public Set<? extends InstanceReference> findInstancesWithLinkTo(String originalParentProperty, InstanceReference originalId, ReferenceType referenceType) {
        Set<String> originalIdsWithLinkTo = repository.findOriginalIdsWithLinkTo(ArangoDocumentReference.fromNexusInstance(castReference(originalId)), ArangoCollectionReference.fromFieldName(originalParentProperty, referenceType), databaseFactory.getDefaultDB());
        return originalIdsWithLinkTo.stream().map(NexusInstanceReference::createFromUrl).collect(Collectors.toSet());
    }

    @Override
    public void mapToOriginalSpace(MainVertex vertex) {
        NexusInstanceReference instanceReference = castReference(vertex.getInstanceReference());
        vertex.toSubSpace(SubSpace.MAIN);
        NexusInstanceReference originalId = castReference(findOriginalId(instanceReference));
        vertex.setInstanceReference(originalId);
        List<Edge> allEdgesByFollowingEmbedded = vertex.getAllEdgesByFollowingEmbedded();
        allEdgesByFollowingEmbedded.stream().filter(e -> e instanceof InternalEdge).forEach(
                e -> {
                    NexusInstanceReference originalReference = repository.findOriginalId(castReference(instanceReference));
                    ((InternalEdge) e).setReference(originalReference);
                }
        );
    }

    @Override
    public Set<VertexOrEdgeReference> getVertexOrEdgeReferences(InstanceReference mainVertex, TargetDatabase database) {
        NexusInstanceReference nexusInstanceReference = castReference(mainVertex);
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

    @Override
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


    private NexusInstanceReference castReference(InstanceReference instanceReference) {
        if (instanceReference instanceof NexusInstanceReference) {
            return (NexusInstanceReference) instanceReference;
        } else {
            throw new WrongReferenceTypeException();
        }
    }


    @Override
    public String getPayloadFromPrimaryStore(InstanceReference instanceReference) {
        return systemNexusClient.getPayload(castReference(instanceReference));
    }

    @Override
    public String getPayloadById(InstanceReference instanceReference, TargetDatabase database) {
        return repository.getPayloadById(ArangoDocumentReference.fromNexusInstance(castReference(instanceReference)), getConnection(database));
    }

    @Override
    public InstanceReference findOriginalId(InstanceReference instanceReference){
        return repository.findOriginalId(castReference(instanceReference));
    }

    @Override
    public Set<InstanceReference> findAllIdsForEntity(InstanceReference anyReference) {
        return null;
    }

}
