package org.humanbrainproject.knowledgegraph.commons.propertyGraph.entity;

import com.github.jsonldjava.core.JsonLdConsts;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusInstanceReference;

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

    private NexusInstanceReference instanceReference;

    @Override
    public MainVertex getMainVertex() {
        return this;
    }

    public MainVertex(NexusInstanceReference instanceReference) {
        super();
        this.instanceReference = instanceReference;
    }

    public void toSubSpace(SubSpace subSpace){
        this.instanceReference = this.instanceReference.toSubSpace(subSpace);
    }

    public void setInstanceReference(NexusInstanceReference instanceReference) {
        this.instanceReference = instanceReference;
    }

    public List<String> getTypes(){
        return getValuesByPropertyName(JsonLdConsts.TYPE);
    }


    public NexusInstanceReference getInstanceReference() {
        return instanceReference;
    }

    public List<Edge> getAllEdgesByFollowingEmbedded(){
        return findEdgesByFollowingEmbedded(this, new ArrayList<>(), null);
    }

    public List<Edge> getEdgesByFollowingEmbedded(List<String> edgeBlacklist){
        return findEdgesByFollowingEmbedded(this, new ArrayList<>(), edgeBlacklist);
    }

    private static List<Edge> findEdgesByFollowingEmbedded(Vertex currentVertex, List<Edge> collector, List<String> edgeBlacklist){
        if(currentVertex!=null){
            List<Edge> edges = currentVertex.getEdges();
            for (Edge edge : edges) {
                if(edgeBlacklist==null || !edgeBlacklist.contains(edge.getName())) {
                    collector.add(edge);
                    if (edge instanceof EmbeddedEdge) {
                        EmbeddedEdge embeddedEdge = (EmbeddedEdge) edge;
                        findEdgesByFollowingEmbedded(embeddedEdge.getToVertex(), collector, edgeBlacklist);
                    }
                }
            }
        }
        return collector;
    }
}
