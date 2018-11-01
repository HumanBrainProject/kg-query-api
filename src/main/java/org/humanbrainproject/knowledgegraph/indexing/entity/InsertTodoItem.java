package org.humanbrainproject.knowledgegraph.indexing.entity;

import org.humanbrainproject.knowledgegraph.commons.propertyGraph.control.DatabaseConnection;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.entity.VertexOrEdge;

public class InsertTodoItem extends TodoItemWithDatabaseConnection{

    private final VertexOrEdge object;

    public InsertTodoItem(VertexOrEdge object, DatabaseConnection<?> databaseConnection) {
        super(databaseConnection);
        this.object = object;
    }

    public VertexOrEdge getObject() {
        return object;
    }
}
