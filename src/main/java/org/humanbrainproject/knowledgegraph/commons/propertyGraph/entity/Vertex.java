package org.humanbrainproject.knowledgegraph.commons.propertyGraph.entity;


import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A vertex is a representation of a simple key-value map, not allowing any nested elements as values but only primitive values or edges to other vertices.
 */
public abstract class Vertex implements VertexOrEdge {


    private final List<Property> properties;
    private final List<Edge> edges;

    public Vertex() {
        this.properties = new ArrayList<>();
        this.edges = new ArrayList<>();
    }

    public List<Property> getProperties() {
        return properties;
    }

    public Property getPropertyByName(String propertyName) {
        for (Property property : properties) {
            if (property.getName().equals(propertyName)) {
                return property;
            }
        }
        return null;
    }

    public List<String> getValuesByPropertyName(String propertyName) {
        Property typeProperty = this.getPropertyByName(propertyName);
        if (typeProperty == null || typeProperty.getValue() == null) {
            return Collections.emptyList();
        } else if (typeProperty.getValue() instanceof Collection) {
            return ((Collection<?>) typeProperty.getValue()).stream().map(e -> e != null ? e.toString() : null).collect(Collectors.toList());
        } else {
            return Collections.singletonList(typeProperty.getValue().toString());
        }
    }

    public List<Edge> getEdges() {
        return edges;
    }

    public Edge getEdgeByName(String edgeName) {
        for (Edge edge : edges) {
            if (edge.getTypeName().equals(edgeName)) {
                return edge;
            }
        }
        return null;
    }

}
