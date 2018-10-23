package org.humanbrainproject.knowledgegraph.propertyGraph.entity;

import java.util.List;
import java.util.Objects;

/**
 * An edge is a representation of a relation between two vertices. It can contain properties itself
 */
public abstract class Edge implements VertexOrEdge{

    private final String name;

    private final Vertex fromVertex;

    private final List<Property> properties;

    /**
     * The orderNumber is an indication in which order the edge has been created relative to its {@link #fromVertex}.
     * Sorting by this value as part of the query allows to keep the order of insertion.
     */
    private final Integer orderNumber;

    public Edge(String name, Vertex fromVertex, List<Property> properties, Integer orderNumber) {
        this.name = name;
        this.properties = properties;
        this.fromVertex = fromVertex;
        this.orderNumber = orderNumber;
    }


    String getIdentifier(){
        return String.format("%s#%s-%d", getFromVertex().getInternalIdentifier(), getName(), getOrderNumber());
    }

    public String getName() {
        return name;
    }

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
                Objects.equals(fromVertex.getInternalIdentifier(), edge.fromVertex.getInternalIdentifier()) &&
                Objects.equals(properties, edge.properties) &&
                Objects.equals(orderNumber, edge.orderNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, fromVertex.getInternalIdentifier(), properties, orderNumber);
    }
}
