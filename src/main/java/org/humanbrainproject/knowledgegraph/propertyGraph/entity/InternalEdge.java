package org.humanbrainproject.knowledgegraph.propertyGraph.entity;

import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusInstanceReference;

import java.util.ArrayList;
import java.util.Objects;

public class InternalEdge extends Edge {

    private NexusInstanceReference reference;

    public InternalEdge(String name, Vertex fromVertex, NexusInstanceReference reference, Integer orderNumber) {
        super(name, fromVertex, new ArrayList<>(), orderNumber);
        this.reference = reference;
    }

    public NexusInstanceReference getReference() {
        return reference;
    }

    public void setReference(NexusInstanceReference reference) {
        this.reference = reference;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        InternalEdge that = (InternalEdge) o;
        return Objects.equals(reference, that.reference);
    }

    @Override
    public int hashCode() {

        return Objects.hash(super.hashCode(), reference);
    }
}
