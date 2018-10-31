package org.humanbrainproject.knowledgegraph.indexing.control;

import org.humanbrainproject.knowledgegraph.commons.propertyGraph.control.DatabaseConnection;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.entity.MainVertex;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.entity.ReferenceType;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.entity.VertexOrEdgeReference;
import org.humanbrainproject.knowledgegraph.indexing.entity.TargetDatabase;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusInstanceReference;

import java.util.Set;

public interface IndexingProvider {

    DatabaseConnection getConnection(TargetDatabase database);

    Set<VertexOrEdgeReference> getVertexOrEdgeReferences(NexusInstanceReference mainVertex, TargetDatabase database);

    MainVertex getVertexStructureById(NexusInstanceReference incomingReference);

    String getPayloadById(NexusInstanceReference instanceReference, TargetDatabase database);

    /**
     * Rewrites the main vertex and its embedded vertices so all internal references are only pointing to original ids and to the main space (no-postfix).
     *
     * @param vertex
     */
    void mapToOriginalSpace(MainVertex vertex, NexusInstanceReference originalReference);

    String getPayloadFromPrimaryStore(NexusInstanceReference instanceReference);

    /**
     *
     * @param instanceReference can be either the reference to the original entity itself or a reference to it's editor / reconciled representation
     * @return the instanceReference to the original instance
     */
    NexusInstanceReference findOriginalId(NexusInstanceReference instanceReference);

    Set<NexusInstanceReference> findInstancesWithLinkTo(String originalParentProperty, NexusInstanceReference originalId, ReferenceType referenceType);
}
