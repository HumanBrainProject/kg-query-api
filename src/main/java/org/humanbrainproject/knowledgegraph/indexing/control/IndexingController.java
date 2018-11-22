package org.humanbrainproject.knowledgegraph.indexing.control;

import org.humanbrainproject.knowledgegraph.commons.authorization.entity.OidcAccessToken;
import org.humanbrainproject.knowledgegraph.indexing.entity.QualifiedIndexingMessage;
import org.humanbrainproject.knowledgegraph.indexing.entity.TodoList;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusInstanceReference;

public interface IndexingController {

    TodoList insert(QualifiedIndexingMessage message, TodoList todoList, OidcAccessToken oidcAccessToken);

    TodoList update(QualifiedIndexingMessage message, TodoList todoList, OidcAccessToken oidcAccessToken);

    TodoList delete(NexusInstanceReference reference, TodoList todoList, OidcAccessToken oidcAccessToken);

    void clear(OidcAccessToken oidcAccessToken);

}
