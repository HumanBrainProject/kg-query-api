package org.humanbrainproject.knowledgegraph.indexing.control;

import org.humanbrainproject.knowledgegraph.indexing.entity.QualifiedIndexingMessage;
import org.humanbrainproject.knowledgegraph.indexing.entity.TodoList;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusInstanceReference;

public interface IndexingController {

    TodoList insert(QualifiedIndexingMessage message, TodoList todoList);

    TodoList update(QualifiedIndexingMessage message, TodoList todoList);

    TodoList delete(NexusInstanceReference reference, TodoList todoList);

    void clear();

}
