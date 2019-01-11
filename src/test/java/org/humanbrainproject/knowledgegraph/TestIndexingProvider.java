package org.humanbrainproject.knowledgegraph;

import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.ArangoConnection;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.entity.Vertex;
import org.humanbrainproject.knowledgegraph.indexing.control.nexusToArango.NexusToArangoIndexingProvider;
import org.humanbrainproject.knowledgegraph.indexing.entity.TargetDatabase;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusInstanceReference;

import java.util.Set;

public class TestIndexingProvider extends NexusToArangoIndexingProvider{

    @Override
    public Set<NexusInstanceReference> findInstancesWithLinkTo(String originalParentProperty, NexusInstanceReference originalId) {
        return super.findInstancesWithLinkTo(originalParentProperty, originalId);
    }

    @Override
    public Vertex mapToOriginalSpace(Vertex vertex, NexusInstanceReference originalId) {
        return super.mapToOriginalSpace(vertex, originalId);
    }

    @Override
    public ArangoConnection getConnection(TargetDatabase database) {
        return super.getConnection(database);
    }

    @Override
    public String getPayloadFromPrimaryStore(NexusInstanceReference instanceReference) {
        return super.getPayloadFromPrimaryStore(instanceReference);
    }

    @Override
    public String getPayloadById(NexusInstanceReference instanceReference, TargetDatabase database) {
        return super.getPayloadById(instanceReference, database);
    }

    @Override
    public NexusInstanceReference findOriginalId(NexusInstanceReference instanceReference) {
        return super.findOriginalId(instanceReference);
    }
}
