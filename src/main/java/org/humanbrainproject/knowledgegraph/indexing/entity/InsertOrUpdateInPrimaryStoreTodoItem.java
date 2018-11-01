package org.humanbrainproject.knowledgegraph.indexing.entity;

import org.humanbrainproject.knowledgegraph.commons.propertyGraph.entity.MainVertex;

public class InsertOrUpdateInPrimaryStoreTodoItem implements TodoItem {

    private final MainVertex object;

    public InsertOrUpdateInPrimaryStoreTodoItem(MainVertex object) {
        this.object = object;
    }

    public MainVertex getObject() {
        return object;
    }

}
