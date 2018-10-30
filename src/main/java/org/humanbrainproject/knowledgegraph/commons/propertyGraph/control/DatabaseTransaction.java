package org.humanbrainproject.knowledgegraph.commons.propertyGraph.control;

import org.humanbrainproject.knowledgegraph.indexing.entity.TodoList;

public interface DatabaseTransaction<T> {

    void execute(TodoList<T> todoList);

}
