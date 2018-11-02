package org.humanbrainproject.knowledgegraph.indexing.entity;

import org.humanbrainproject.knowledgegraph.commons.propertyGraph.entity.Vertex;

public class InsertOrUpdateInPrimaryStoreTodoItem implements TodoItem {

    private final Vertex vertex;

    public InsertOrUpdateInPrimaryStoreTodoItem(Vertex object) {
        this.vertex = object;
    }

    public Vertex getVertex() {
        return vertex;
    }

}
