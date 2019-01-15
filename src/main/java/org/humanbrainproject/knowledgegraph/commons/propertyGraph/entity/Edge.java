package org.humanbrainproject.knowledgegraph.commons.propertyGraph.entity;

import org.humanbrainproject.knowledgegraph.annotations.ToBeTested;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusInstanceReference;

@ToBeTested(easy = true)
public class Edge implements VertexOrEdge{

    private final JsonPath path;
    private NexusInstanceReference reference;
    private Vertex vertex;

    public Edge(Vertex vertex, JsonPath path, NexusInstanceReference reference) {
        this.path = path;
        this.reference = reference;
        this.vertex = vertex;
    }

    public JsonPath getPath() {
        return path;
    }

    public NexusInstanceReference getReference() {
        return reference;
    }

    public void setReference(NexusInstanceReference reference) {
        this.reference = reference;
    }

    public String getId(){
        StringBuilder sb = new StringBuilder();
        sb.append(vertex.getInstanceReference().getFullId(false));
        for (Step step : path) {
            sb.append('-').append(step.getName()).append('-').append(step.getOrderNumber());
        }
        return sb.toString();
    }

    //FIXME: This is not the proper way of handling the order number since it doesn't support multi-level nesting
    public Integer getLastOrderNumber(){
        if(path.isEmpty()){
            return null;
        }
        return path.get(path.size()-1).getOrderNumber();
    }

    public String getName(){
        if(path.isEmpty()){
            return null;
        }
        return path.get(path.size()-1).getName();
    }

}
