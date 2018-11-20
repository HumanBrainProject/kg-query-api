package org.humanbrainproject.knowledgegraph.indexing.entity;

import org.humanbrainproject.knowledgegraph.commons.propertyGraph.control.DatabaseConnection;

public abstract class TodoItemWithDatabaseConnection implements TodoItem{


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
