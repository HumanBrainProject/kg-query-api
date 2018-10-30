package org.humanbrainproject.knowledgegraph.commons.propertyGraph.entity;

import java.util.ArrayList;
import java.util.Objects;

public class EmbeddedEdge extends Edge {


    private Vertex toVertex;

    public EmbeddedEdge(String name, Vertex fromVertex, Integer orderNumber, MainVertex mainVertex) {
        super(name, fromVertex, new ArrayList<>(), orderNumber, mainVertex, ReferenceType.EMBEDDED);
    }

    public void setToVertex(Vertex toVertex) {
        this.toVertex = toVertex;
    }

    public Vertex getToVertex() {
        return toVertex;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        EmbeddedEdge that = (EmbeddedEdge) o;
        return Objects.equals(toVertex.getTypeName(), that.toVertex.getTypeName()) && Objects.equals(toVertex.getId(), that.toVertex.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), toVertex.getTypeName(), toVertex.getId());
    }
}
