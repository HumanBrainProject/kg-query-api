package org.humanbrainproject.knowledgegraph.indexing.entity;

import org.humanbrainproject.knowledgegraph.commons.propertyGraph.control.DatabaseConnection;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.entity.VertexOrEdge;

public class InsertTodoItem<T> implements TodoItem{

    private final VertexOrEdge object;
    private final DatabaseConnection<T> databaseConnection;

    public InsertTodoItem(VertexOrEdge object, DatabaseConnection<T> databaseConnection) {
        this.object = object;
        this.databaseConnection = databaseConnection;
    }

    public VertexOrEdge getObject() {
        return object;
    }

    public DatabaseConnection<T> getDatabaseConnection() {
        return databaseConnection;
    }
}
