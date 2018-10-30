package org.humanbrainproject.knowledgegraph.commons.propertyGraph.entity;

import com.github.jsonldjava.core.JsonLdConsts;
import org.humanbrainproject.knowledgegraph.indexing.entity.InstanceReference;

import java.util.ArrayList;
import java.util.List;

public class MainVertex extends Vertex {

    @Override
    public String getId() {
        return instanceReference.getId();
    }

    @Override
    public String getTypeName() {
        return instanceReference.getTypeName();
    }

    private InstanceReference instanceReference;

    @Override
    public MainVertex getMainVertex() {
        return this;
    }

    public MainVertex(InstanceReference instanceReference) {
        super();
        this.instanceReference = instanceReference;
    }

    public void toSubSpace(SubSpace subSpace){
        this.instanceReference = this.instanceReference.toSubSpace(subSpace);
    }

    public void setInstanceReference(InstanceReference instanceReference) {
        this.instanceReference = instanceReference;
    }

    public List<String> getTypes(){
        return getValuesByPropertyName(JsonLdConsts.TYPE);
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
