package org.humanbrainproject.knowledgegraph.indexing.control;

import org.humanbrainproject.knowledgegraph.indexing.entity.QualifiedIndexingMessage;
import org.humanbrainproject.knowledgegraph.indexing.entity.TodoList;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusInstanceReference;

import java.io.IOException;

public interface IndexingController {

    <T> TodoList<T> insert(QualifiedIndexingMessage message, TodoList<T> todoList) throws IOException;

    <T> TodoList<T> update(QualifiedIndexingMessage message, TodoList<T> todoList) throws IOException;

    <T> TodoList<T> delete(NexusInstanceReference reference, TodoList<T> todoList);

    void clear();

}
