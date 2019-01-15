package org.humanbrainproject.knowledgegraph.indexing.control;

import org.humanbrainproject.knowledgegraph.annotations.NoTests;
import org.humanbrainproject.knowledgegraph.indexing.entity.QualifiedIndexingMessage;
import org.humanbrainproject.knowledgegraph.indexing.entity.todo.TodoList;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusInstanceReference;

@NoTests(NoTests.NO_LOGIC)
public interface IndexingController {

    TodoList insert(QualifiedIndexingMessage message, TodoList todoList);

    TodoList update(QualifiedIndexingMessage message, TodoList todoList);

    TodoList delete(NexusInstanceReference reference, TodoList todoList);

    void clear();

}
