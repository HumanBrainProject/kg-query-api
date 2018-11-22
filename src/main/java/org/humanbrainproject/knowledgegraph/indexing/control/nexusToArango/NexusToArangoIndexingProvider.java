package org.humanbrainproject.knowledgegraph.indexing.control.nexusToArango;

import org.humanbrainproject.knowledgegraph.commons.authorization.entity.OidcAccessToken;
import org.humanbrainproject.knowledgegraph.commons.jsonld.control.JsonLdStandardization;
import org.humanbrainproject.knowledgegraph.commons.nexus.control.SystemNexusClient;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.ArangoConnection;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.ArangoDatabaseFactory;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.ArangoRepository;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoCollectionReference;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoDocumentReference;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.entity.Edge;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.entity.SubSpace;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.entity.Vertex;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.entity.VertexOrEdgeReference;
import org.humanbrainproject.knowledgegraph.indexing.control.MessageProcessor;
import org.humanbrainproject.knowledgegraph.indexing.entity.IndexingMessage;
import org.humanbrainproject.knowledgegraph.indexing.entity.QualifiedIndexingMessage;
import org.humanbrainproject.knowledgegraph.indexing.entity.TargetDatabase;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusInstanceReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
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

    @Autowired
    JsonLdStandardization jsonLdStandardization;

    public Vertex getVertexStructureById(NexusInstanceReference incomingReference) {
        String payload = systemNexusClient.getPayload(incomingReference);
        QualifiedIndexingMessage qualifiedMessage = messageProcessor.qualify(new IndexingMessage(incomingReference, payload, null, null));
        return messageProcessor.createVertexStructure(qualifiedMessage);
    }

    public Set<NexusInstanceReference> findInstancesWithLinkTo(String originalParentProperty, NexusInstanceReference originalId, OidcAccessToken oidcAccessToken) {
        return repository.findOriginalIdsWithLinkTo(ArangoDocumentReference.fromNexusInstance(originalId), ArangoCollectionReference.fromFieldName(originalParentProperty), databaseFactory.getDefaultDB(), oidcAccessToken);
    }

    public Vertex mapToOriginalSpace(Vertex vertex, NexusInstanceReference originalId, OidcAccessToken oidcAccessToken) {
        QualifiedIndexingMessage newMessage = new QualifiedIndexingMessage(vertex.getQualifiedIndexingMessage().getOriginalMessage(), new LinkedHashMap(vertex.getQualifiedIndexingMessage().getQualifiedMap()));
        Vertex newVertex = messageProcessor.createVertexStructure(newMessage);
        Map<NexusInstanceReference, NexusInstanceReference> toOriginalIdMap = new HashMap<>();
        for (Edge edge : newVertex.getEdges()) {
            NexusInstanceReference relatedOriginalId = repository.findOriginalId(edge.getReference(), oidcAccessToken);
            relatedOriginalId = relatedOriginalId.toSubSpace(SubSpace.MAIN);
            toOriginalIdMap.put(edge.getReference(), relatedOriginalId);
            edge.setReference(relatedOriginalId);
        }
        newVertex.setInstanceReference(originalId);
        newVertex.toSubSpace(SubSpace.MAIN);
        //jsonLdStandardization.extendInternalReferencesWithRelativeUrl(newVertex.getQualifiedIndexingMessage().getQualifiedMap(), toOriginalIdMap::get);
        return newVertex;
    }


    public Set<VertexOrEdgeReference> getVertexOrEdgeReferences(NexusInstanceReference nexusInstanceReference, TargetDatabase database, OidcAccessToken oidcAccessToken) {
        Set<ArangoDocumentReference> referencesBelongingToInstance = repository.getReferencesBelongingToInstance(nexusInstanceReference, getConnection(database), oidcAccessToken);
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

    public String getPayloadById(NexusInstanceReference instanceReference, TargetDatabase database, OidcAccessToken oidcAccessToken) {
        return repository.getPayloadById(ArangoDocumentReference.fromNexusInstance(instanceReference), getConnection(database), oidcAccessToken);
    }

    public NexusInstanceReference findOriginalId(NexusInstanceReference instanceReference, OidcAccessToken oidcAccessToken){
        return repository.findOriginalId(instanceReference, oidcAccessToken);
    }
}
