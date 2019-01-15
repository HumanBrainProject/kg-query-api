package org.humanbrainproject.knowledgegraph.indexing.control.basic;

import org.humanbrainproject.knowledgegraph.annotations.ToBeTested;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.entity.Vertex;
import org.humanbrainproject.knowledgegraph.indexing.control.IndexingController;
import org.humanbrainproject.knowledgegraph.indexing.control.MessageProcessor;
import org.humanbrainproject.knowledgegraph.indexing.control.nexusToArango.NexusToArangoIndexingProvider;
import org.humanbrainproject.knowledgegraph.indexing.entity.*;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusInstanceReference;
import org.humanbrainproject.knowledgegraph.indexing.entity.todo.DeleteTodoItem;
import org.humanbrainproject.knowledgegraph.indexing.entity.todo.InsertTodoItem;
import org.humanbrainproject.knowledgegraph.indexing.entity.todo.TodoList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * The basic indexing controller ensures the insertion of the incoming message into the native / original database
 */
@ToBeTested
@Component
public class BasicIndexingController implements IndexingController {

    @Autowired
    MessageProcessor messageProcessor;

    @Autowired
    NexusToArangoIndexingProvider indexingProvider;


    @Override
    public TodoList insert(QualifiedIndexingMessage message, TodoList todoList){
        Vertex vertex = messageProcessor.createVertexStructure(message);
        InsertTodoItem insertTodoItem = new InsertTodoItem(vertex, indexingProvider.getConnection(TargetDatabase.NATIVE));
        todoList.addTodoItem(insertTodoItem);
        return todoList;
    }

    @Override
    public TodoList update(QualifiedIndexingMessage message, TodoList todoList){
        //TODO transfer information about creation / previous authors to new message?
        insert(message, todoList);
        return todoList;
    }

    @Override
    public TodoList delete(NexusInstanceReference reference, TodoList todoList) {
        todoList.addTodoItem(new DeleteTodoItem(reference,  indexingProvider.getConnection(TargetDatabase.NATIVE)));
        return todoList;
    }

    @Override
    public void clear() {
        indexingProvider.getConnection(TargetDatabase.NATIVE).clearData();
    }
}
