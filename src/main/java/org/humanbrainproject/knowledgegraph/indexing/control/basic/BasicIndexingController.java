package org.humanbrainproject.knowledgegraph.indexing.control.basic;

import org.humanbrainproject.knowledgegraph.commons.authorization.entity.Credential;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.entity.Vertex;
import org.humanbrainproject.knowledgegraph.indexing.control.IndexingController;
import org.humanbrainproject.knowledgegraph.indexing.control.MessageProcessor;
import org.humanbrainproject.knowledgegraph.indexing.control.nexusToArango.NexusToArangoIndexingProvider;
import org.humanbrainproject.knowledgegraph.indexing.entity.*;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusInstanceReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class BasicIndexingController implements IndexingController {

    @Autowired
    MessageProcessor messageProcessor;

    @Autowired
    NexusToArangoIndexingProvider indexingProvider;


    @Override
    public TodoList insert(QualifiedIndexingMessage message, TodoList todoList, Credential credential){
        Vertex vertex = messageProcessor.createVertexStructure(message);
        InsertTodoItem insertTodoItem = new InsertTodoItem(vertex, indexingProvider.getConnection(TargetDatabase.DEFAULT));
        todoList.addTodoItem(insertTodoItem);
        return todoList;
    }

    @Override
    public TodoList update(QualifiedIndexingMessage message, TodoList todoList, Credential credential){
        //TODO transfer information about creation / previous authors to new message?
        insert(message, todoList, credential);
        return todoList;
    }

    @Override
    public TodoList delete(NexusInstanceReference reference, TodoList todoList, Credential credential) {
        todoList.addTodoItem(new DeleteTodoItem(reference,  indexingProvider.getConnection(TargetDatabase.DEFAULT)));
        return todoList;
    }

    @Override
    public void clear(Credential credential) {
        indexingProvider.getConnection(TargetDatabase.DEFAULT).clearData();
    }
}
