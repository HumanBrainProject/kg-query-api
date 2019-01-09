package org.humanbrainproject.knowledgegraph.commons.propertyGraph.control;

import org.humanbrainproject.knowledgegraph.annotations.NoTests;
import org.humanbrainproject.knowledgegraph.indexing.entity.todo.TodoList;

@NoTests(NoTests.NO_LOGIC)
public interface DatabaseTransaction {

    void execute(TodoList todoList);

}
