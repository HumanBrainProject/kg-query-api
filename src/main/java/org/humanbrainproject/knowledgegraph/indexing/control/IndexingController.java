package org.humanbrainproject.knowledgegraph.indexing.control;

import org.humanbrainproject.knowledgegraph.indexing.entity.InstanceReference;
import org.humanbrainproject.knowledgegraph.indexing.entity.QualifiedIndexingMessage;
import org.humanbrainproject.knowledgegraph.indexing.entity.TodoList;

public interface IndexingController {

    void insert(QualifiedIndexingMessage message, TodoList todoList);

    void update(QualifiedIndexingMessage message, TodoList todoList);

    void delete(InstanceReference reference, TodoList todoList);

    void clear();

}
