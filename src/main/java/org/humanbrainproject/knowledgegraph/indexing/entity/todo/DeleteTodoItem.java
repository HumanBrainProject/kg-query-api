package org.humanbrainproject.knowledgegraph.indexing.entity.todo;

import org.humanbrainproject.knowledgegraph.annotations.NoTests;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.control.DatabaseConnection;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusInstanceReference;

@NoTests(NoTests.NO_LOGIC)
public class DeleteTodoItem extends TodoItemWithDatabaseConnection {

    private final NexusInstanceReference reference;

    public DeleteTodoItem(NexusInstanceReference reference, DatabaseConnection<?> databaseConnection) {
        super(databaseConnection);
        this.reference = reference;
    }

    public NexusInstanceReference getReference() {
        return reference;
    }


}
