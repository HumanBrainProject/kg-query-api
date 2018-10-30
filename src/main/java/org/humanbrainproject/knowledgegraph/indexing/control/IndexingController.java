package org.humanbrainproject.knowledgegraph.indexing.control;

import org.humanbrainproject.knowledgegraph.indexing.entity.InstanceReference;
import org.humanbrainproject.knowledgegraph.indexing.entity.QualifiedIndexingMessage;
import org.humanbrainproject.knowledgegraph.indexing.entity.TodoList;

import java.io.IOException;

public interface IndexingController {

    <T> TodoList<T> insert(QualifiedIndexingMessage message, TodoList<T> todoList) throws IOException;

    <T> TodoList<T> update(QualifiedIndexingMessage message, TodoList<T> todoList) throws IOException;

    <T> TodoList<T> delete(InstanceReference reference, TodoList<T> todoList);

    void clear();

}
