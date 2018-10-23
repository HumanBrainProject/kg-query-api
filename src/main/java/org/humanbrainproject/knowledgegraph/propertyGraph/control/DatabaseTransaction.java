package org.humanbrainproject.knowledgegraph.propertyGraph.control;

import org.humanbrainproject.knowledgegraph.indexing.entity.TodoList;

public interface DatabaseTransaction {

    void execute(TodoList todoList);

}
