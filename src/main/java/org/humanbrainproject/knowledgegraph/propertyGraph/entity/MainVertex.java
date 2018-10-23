package org.humanbrainproject.knowledgegraph.propertyGraph.entity;

import org.humanbrainproject.knowledgegraph.indexing.entity.InstanceReference;

import java.util.ArrayList;
import java.util.List;

public class MainVertex extends Vertex {

    private InstanceReference instanceReference;

    public MainVertex(InstanceReference instanceReference) {
        super();
        this.instanceReference = instanceReference;
    }

    @Override
    String getInternalIdentifier() {
        return getInstanceReference().getInternalIdentifier();
    }

    public void toSubSpace(SubSpace subSpace){
        this.instanceReference = this.instanceReference.toSubSpace(subSpace);
    }

    public void setInstanceReference(InstanceReference instanceReference) {
        this.instanceReference = instanceReference;
    }

    public InstanceReference getInstanceReference() {
        return instanceReference;
    }


    public List<Edge> getAllEdgesByFollowingEmbedded(){
        return findEdgesByFollowingEmbedded(this, new ArrayList<>());
    }

    private static List<Edge> findEdgesByFollowingEmbedded(Vertex currentVertex, List<Edge> collector){
        if(currentVertex!=null){
            List<Edge> edges = currentVertex.getEdges();
            for (Edge edge : edges) {
                collector.add(edge);
                if(edge instanceof EmbeddedEdge){
                    EmbeddedEdge embeddedEdge = (EmbeddedEdge)edge;
                    findEdgesByFollowingEmbedded(embeddedEdge.getToVertex(), collector);
                }
            }
        }
        return collector;
    }
}
