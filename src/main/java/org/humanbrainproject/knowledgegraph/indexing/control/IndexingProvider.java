package org.humanbrainproject.knowledgegraph.indexing.control;

import org.humanbrainproject.knowledgegraph.commons.propertyGraph.control.DatabaseConnection;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.entity.MainVertex;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.entity.ReferenceType;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.entity.VertexOrEdgeReference;
import org.humanbrainproject.knowledgegraph.indexing.entity.InstanceReference;
import org.humanbrainproject.knowledgegraph.indexing.entity.TargetDatabase;

import java.util.Set;

public interface IndexingProvider {

    DatabaseConnection getConnection(TargetDatabase database);

    Set<VertexOrEdgeReference> getVertexOrEdgeReferences(InstanceReference mainVertex, TargetDatabase database);

    MainVertex getVertexStructureById(InstanceReference incomingReference);

//    MainVertex getVertexStructureById(InstanceReference incomingReference, TargetDatabase database);
//
//    MainVertex getVertexStructureById(InstanceReference incomingReference, TargetDatabase database, SubSpace targetSpace);

    String getPayloadById(InstanceReference instanceReference, TargetDatabase database);

    /**
     * Rewrites the main vertex and its embedded vertices so all internal references are only pointing to original ids and to the main space (no-postfix).
     *
     * @param vertex
     */
    void mapToOriginalSpace(MainVertex vertex);

    String getPayloadFromPrimaryStore(InstanceReference instanceReference);

    /**
     *
     * @param instanceReference can be either the reference to the original entity itself or a reference to it's editor / reconciled representation
     * @return the instanceReference to the original instance
     */
    InstanceReference findOriginalId(InstanceReference instanceReference);

    Set<InstanceReference> findAllIdsForEntity(InstanceReference anyReference);

    Set<? extends InstanceReference> findInstancesWithLinkTo(String originalParentProperty, InstanceReference originalId, ReferenceType referenceType);
}
