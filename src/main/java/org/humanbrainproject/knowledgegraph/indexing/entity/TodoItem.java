package org.humanbrainproject.knowledgegraph.indexing.entity;

import org.humanbrainproject.knowledgegraph.propertyGraph.control.DatabaseConnection;
import org.humanbrainproject.knowledgegraph.propertyGraph.entity.VertexOrEdge;

public class TodoItem {

    public enum Action{
        INSERT, UPDATE, DELETE, INSERT_OR_UPDATE_IN_PRIMARY_STORE;
    }

    private final VertexOrEdge object;
    private final DatabaseConnection databaseConnection;
    private final Action action;

    public TodoItem(VertexOrEdge object, DatabaseConnection databaseConnection, Action action) {
        this.object = object;
        this.databaseConnection = databaseConnection;
        this.action = action;
    }

    public VertexOrEdge getObject() {
        return object;
    }

    public DatabaseConnection getDatabaseConnection() {
        return databaseConnection;
    }

    public Action getAction() {
        return action;
    }
}
