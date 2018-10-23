package org.humanbrainproject.knowledgegraph.propertyGraph.entity;


import java.util.*;

/**
 * A vertex is a representation of a simple key-value map, not allowing any nested elements as values but only primitive values or edges to other vertices.
 */
public abstract class Vertex implements VertexOrEdge{


    private final List<Property> properties;
    private final List<Edge> edges;

    public Vertex() {
        this.properties = new ArrayList<>();
        this.edges = new ArrayList<>();
    }

    public List<Property> getProperties() {
        return properties;
    }

    public List<Edge> getEdges() {
        return edges;
    }

    abstract String getInternalIdentifier() ;
}
