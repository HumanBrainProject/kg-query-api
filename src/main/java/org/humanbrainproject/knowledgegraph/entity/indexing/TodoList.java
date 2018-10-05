package org.humanbrainproject.knowledgegraph.entity.indexing;

import org.humanbrainproject.knowledgegraph.entity.jsonld.JsonLdEdge;
import org.humanbrainproject.knowledgegraph.entity.jsonld.JsonLdVertex;

import java.util.LinkedHashSet;
import java.util.Set;

public class TodoList {

    private final Set<JsonLdVertex> verticesToBeCreated = new LinkedHashSet<>();
    private final Set<JsonLdEdge> edgesToBeCreated = new LinkedHashSet<>();
    private final Set<JsonLdVertex> verticesToBeUpdated = new LinkedHashSet<>();
    private final Set<JsonLdEdge> edgesToBeUpdated = new LinkedHashSet<>();
    private final Set<JsonLdVertex> verticesToBeRemoved = new LinkedHashSet<>();
    private final Set<JsonLdEdge> edgesToBeRemoved = new LinkedHashSet<>();

    public Set<JsonLdVertex> getVerticesToBeCreated() {
        return verticesToBeCreated;
    }

    public Set<JsonLdEdge> getEdgesToBeCreated() {
        return edgesToBeCreated;
    }

    public Set<JsonLdVertex> getVerticesToBeUpdated() {
        return verticesToBeUpdated;
    }

    public Set<JsonLdEdge> getEdgesToBeUpdated() {
        return edgesToBeUpdated;
    }

    public Set<JsonLdVertex> getVerticesToBeRemoved() {
        return verticesToBeRemoved;
    }

    public Set<JsonLdEdge> getEdgesToBeRemoved() {
        return edgesToBeRemoved;
    }
}
