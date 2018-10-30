package org.humanbrainproject.knowledgegraph.indexing.entity;

import org.humanbrainproject.knowledgegraph.commons.propertyGraph.control.DatabaseConnection;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.entity.VertexOrEdgeReference;

public class DeleteTodoItem<T> implements TodoItem{

    private final VertexOrEdgeReference reference;

    private final DatabaseConnection<T> databaseConnection;

    public DeleteTodoItem(VertexOrEdgeReference reference, DatabaseConnection<T> databaseConnection) {
        this.reference = reference;
        this.databaseConnection = databaseConnection;
    }

    public VertexOrEdgeReference getReference() {
        return reference;
    }

    public DatabaseConnection<T> getDatabaseConnection() {
        return databaseConnection;
    }
}
