package org.humanbrainproject.knowledgegraph.commons.propertyGraph.entity;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * An edge is a representation of a relation between two vertices. It can contain properties itself
 */
public abstract class Edge implements VertexOrEdge {

    private final String name;

    private final Vertex fromVertex;

    private final List<Property> properties;

    private final ReferenceType referenceType;

    /**
     * The orderNumber is an indication in which order the edge has been created relative to its {@link #fromVertex}.
     * Sorting by this value as part of the query allows to keep the order of insertion.
     */
    private final Integer orderNumber;

    private final MainVertex mainVertex;

    public Edge(String name, Vertex fromVertex, List<Property> properties, Integer orderNumber, MainVertex mainVertex, ReferenceType referenceType) {
        this.name = name;
        this.properties = properties;
        this.fromVertex = fromVertex;
        this.orderNumber = orderNumber;
        this.mainVertex = mainVertex;
        this.referenceType = referenceType;
    }

    @Override
    public MainVertex getMainVertex() {
        return mainVertex;
    }

    @Override
    public String getId() {
        return String.format("%s-%d", fromVertex.getId(), getOrderNumber());
    }

    public String getName() {
        return name;
    }

    @Override
    public String getTypeName() {
        return String.format("%s%s", getNamePrefix(), getName());
    }

    protected String getNamePrefix(){
        return String.format("%s-", referenceType.getPrefix());
    };

    public Vertex getFromVertex() {
        return fromVertex;
    }

    public List<Property> getProperties() {
        return properties;
    }

    public Integer getOrderNumber() {
        return orderNumber;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Edge edge = (Edge) o;
        return Objects.equals(name, edge.name) &&
                Objects.equals(fromVertex.getTypeName(), edge.fromVertex.getTypeName()) &&
                Objects.equals(fromVertex.getId(), edge.fromVertex.getId()) &&
                Objects.equals(properties, edge.properties) &&
                Objects.equals(orderNumber, edge.orderNumber) && Objects.equals(referenceType, edge.referenceType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, fromVertex.getTypeName(), fromVertex.getId(), properties, orderNumber, referenceType);
    }

    @Override
    public List<Edge> getEdges() {
        return Collections.emptyList();
    }
}
