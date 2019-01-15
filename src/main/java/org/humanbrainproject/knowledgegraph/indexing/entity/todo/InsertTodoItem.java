package org.humanbrainproject.knowledgegraph.indexing.entity.todo;

import org.humanbrainproject.knowledgegraph.annotations.NoTests;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.control.DatabaseConnection;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.entity.JsonPath;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.entity.Vertex;

import java.util.HashSet;
import java.util.Set;

@NoTests(NoTests.NO_LOGIC)
public class InsertTodoItem extends TodoItemWithDatabaseConnection {

    private final Vertex vertex;
    private Set<JsonPath> blacklist = new HashSet<>();

    public InsertTodoItem(Vertex vertex, DatabaseConnection<?> databaseConnection) {
        super(databaseConnection);
        this.vertex = vertex;
    }

    public Vertex getVertex() {
        return vertex;
    }

    public Set<JsonPath> getBlacklist() {
        return blacklist;
    }
}
