package org.humanbrainproject.knowledgegraph.indexing.control;

import org.humanbrainproject.knowledgegraph.commons.authorization.entity.Credential;
import org.humanbrainproject.knowledgegraph.indexing.entity.QualifiedIndexingMessage;
import org.humanbrainproject.knowledgegraph.indexing.entity.TodoList;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusInstanceReference;

public interface IndexingController {

    TodoList insert(QualifiedIndexingMessage message, TodoList todoList, Credential credential);

    TodoList update(QualifiedIndexingMessage message, TodoList todoList, Credential credential);

    TodoList delete(NexusInstanceReference reference, TodoList todoList, Credential credential);

    void clear(Credential credential);

}
