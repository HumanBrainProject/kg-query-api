package org.humanbrainproject.knowledgegraph.commons.propertyGraph.entity;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * An edge is a representation of a relation between two vertices. It can contain properties itself
 */
public class SortedEdgeGroup {

    private final String name;
    private final List<Edge> edges;

    public SortedEdgeGroup(Set<Edge> edges) {
        this.name = edges!=null && !edges.isEmpty() ? edges.iterator().next().getName() : null;
        this.edges = edges.stream().filter(Objects::nonNull).collect(Collectors.toList());
        this.edges.sort((edge, t1) -> {
            if (edge.getOrderNumber() == null) {
                return t1.getOrderNumber() == null ? 0 : -(t1.getOrderNumber().compareTo(edge.getOrderNumber()));
            } else {
                return edge.getOrderNumber().compareTo(t1.getOrderNumber());
            }
        });
    }

    public String getName() {
        return name;
    }

    public List<Edge> getEdges() {
        return edges;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SortedEdgeGroup that = (SortedEdgeGroup) o;
        return Objects.equals(name, that.name) &&
                Objects.equals(edges, that.edges);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, edges);
    }
}
