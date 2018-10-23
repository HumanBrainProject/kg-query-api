package org.humanbrainproject.knowledgegraph.propertyGraph.entity;

import java.util.Collections;
import java.util.Objects;

public class EmbeddedEdge extends Edge {

    private Vertex toVertex;

    public EmbeddedEdge(String name, Vertex fromVertex, Integer orderNumber) {
        super(name, fromVertex, Collections.emptyList(), orderNumber);
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
        return Objects.equals(toVertex.getInternalIdentifier(), that.toVertex.getInternalIdentifier());
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), toVertex.getInternalIdentifier());
    }
}
