package org.humanbrainproject.knowledgegraph.indexing.entity;

import org.humanbrainproject.knowledgegraph.commons.propertyGraph.control.DatabaseConnection;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusInstanceReference;

public class DeleteTodoItem extends TodoItemWithDatabaseConnection{

    private final NexusInstanceReference reference;

    public DeleteTodoItem(NexusInstanceReference reference, DatabaseConnection<?> databaseConnection) {
        super(databaseConnection);
        this.reference = reference;
    }

    public NexusInstanceReference getReference() {
        return reference;
    }


}
