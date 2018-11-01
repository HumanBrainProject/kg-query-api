package org.humanbrainproject.knowledgegraph.indexing.control;

import org.humanbrainproject.knowledgegraph.indexing.entity.QualifiedIndexingMessage;
import org.humanbrainproject.knowledgegraph.indexing.entity.TodoList;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusInstanceReference;

import java.io.IOException;

public interface IndexingController {

    TodoList insert(QualifiedIndexingMessage message, TodoList todoList) throws IOException;

    TodoList update(QualifiedIndexingMessage message, TodoList todoList) throws IOException;

    TodoList delete(NexusInstanceReference reference, TodoList todoList, String timestamp, String userId);

    void clear();

}
