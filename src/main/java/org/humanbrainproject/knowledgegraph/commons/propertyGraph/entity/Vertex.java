package org.humanbrainproject.knowledgegraph.commons.propertyGraph.entity;

import org.humanbrainproject.knowledgegraph.indexing.entity.QualifiedIndexingMessage;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusInstanceReference;

import java.util.ArrayList;
import java.util.List;

public class Vertex implements VertexOrEdge {

    private final List<EdgeX> edges;

    private final QualifiedIndexingMessage qualifiedIndexingMessage;

    private NexusInstanceReference instanceReference;

    public Vertex(QualifiedIndexingMessage qualifiedIndexingMessage) {
        this.qualifiedIndexingMessage = qualifiedIndexingMessage;
        this.instanceReference = qualifiedIndexingMessage.getOriginalMessage().getInstanceReference();
        this.edges = new ArrayList<>();
    }

    public void toSubSpace(SubSpace subSpace){
        this.instanceReference = this.instanceReference.toSubSpace(subSpace);
    }

    public void setInstanceReference(NexusInstanceReference instanceReference) {
        this.instanceReference = instanceReference;
    }

    public NexusInstanceReference getInstanceReference() {
        return instanceReference;
    }

    public List<EdgeX> getEdges() {
        return edges;
    }

    public QualifiedIndexingMessage getQualifiedIndexingMessage() {
        return qualifiedIndexingMessage;
    }
}
