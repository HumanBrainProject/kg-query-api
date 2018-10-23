package org.humanbrainproject.knowledgegraph.nexusToArangoIndexing.control.basic;

import org.humanbrainproject.knowledgegraph.indexing.control.IndexingProvider;
import org.humanbrainproject.knowledgegraph.indexing.entity.InstanceReference;
import org.humanbrainproject.knowledgegraph.indexing.entity.TargetDatabase;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusInstanceReference;
import org.humanbrainproject.knowledgegraph.indexing.exception.WrongReferenceTypeException;
import org.humanbrainproject.knowledgegraph.nexus.control.NexusClient;
import org.humanbrainproject.knowledgegraph.propertyGraph.arango.control.ArangoConnection;
import org.humanbrainproject.knowledgegraph.propertyGraph.arango.control.ArangoDatabaseFactory;
import org.humanbrainproject.knowledgegraph.propertyGraph.arango.control.ArangoRepository;
import org.humanbrainproject.knowledgegraph.propertyGraph.arango.entity.ArangoDocumentReference;
import org.humanbrainproject.knowledgegraph.propertyGraph.entity.Edge;
import org.humanbrainproject.knowledgegraph.propertyGraph.entity.InternalEdge;
import org.humanbrainproject.knowledgegraph.propertyGraph.entity.MainVertex;
import org.humanbrainproject.knowledgegraph.propertyGraph.entity.SubSpace;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Primary
@Component
public class NexusToArangoBasicIndexingController implements IndexingProvider<ArangoConnection> {

    @Autowired
    ArangoDatabaseFactory databaseFactory;

    @Autowired
    ArangoRepository repository;

    @Autowired
    NexusClient nexusClient;

    @Override
    public Set<InstanceReference> findInstancesWithLinkTo(String originalParentProperty, InstanceReference originalId) {
        return null;
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
        return nexusClient.getPayloadWithTechnicalUser(castReference(instanceReference));
    }

    @Override
    public MainVertex getVertexStructureById(InstanceReference instanceReference, TargetDatabase database) {
        return getVertexStructureById(instanceReference, database, null);
    }

    @Override
    public MainVertex getVertexStructureById(InstanceReference instanceReference, TargetDatabase database, SubSpace targetSpace) {
        return repository.getVertexStructureById(ArangoDocumentReference.fromNexusInstance(castReference(instanceReference), targetSpace), getConnection(database));
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
