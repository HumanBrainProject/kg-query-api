package org.humanbrainproject.knowledgegraph.indexing.entity.todo;

import org.humanbrainproject.knowledgegraph.annotations.NoTests;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.control.DatabaseConnection;

@NoTests(NoTests.TRIVIAL)
public abstract class TodoItemWithDatabaseConnection implements TodoItem {


    private final DatabaseConnection<?> databaseConnection;

    public TodoItemWithDatabaseConnection(DatabaseConnection<?> databaseConnection) {
        this.databaseConnection = databaseConnection;
    }

    public <T extends DatabaseConnection<?>> T getDatabaseConnection(Class<T> clazz) {
        if(clazz.isInstance(databaseConnection)){
            return (T)databaseConnection;
        }
        return null;
    }
}
