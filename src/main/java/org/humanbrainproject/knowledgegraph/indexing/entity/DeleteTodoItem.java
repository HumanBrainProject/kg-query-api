package org.humanbrainproject.knowledgegraph.indexing.entity;

import org.humanbrainproject.knowledgegraph.commons.propertyGraph.control.DatabaseConnection;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.entity.VertexOrEdgeReference;

public class DeleteTodoItem extends TodoItemWithDatabaseConnection{

    private final VertexOrEdgeReference reference;

    public DeleteTodoItem(VertexOrEdgeReference reference, DatabaseConnection<?> databaseConnection) {
        super(databaseConnection);
        this.reference = reference;
    }

    public VertexOrEdgeReference getReference() {
        return reference;
    }


}
