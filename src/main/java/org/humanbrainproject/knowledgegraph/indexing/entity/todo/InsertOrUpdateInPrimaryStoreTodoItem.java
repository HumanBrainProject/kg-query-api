package org.humanbrainproject.knowledgegraph.indexing.entity.todo;

import org.humanbrainproject.knowledgegraph.annotations.NoTests;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.entity.Vertex;

@NoTests(NoTests.NO_LOGIC)
public class InsertOrUpdateInPrimaryStoreTodoItem implements TodoItem {

    private final Vertex vertex;

    public InsertOrUpdateInPrimaryStoreTodoItem(Vertex object) {
        this.vertex = object;
    }

    public Vertex getVertex() {
        return vertex;
    }

}
