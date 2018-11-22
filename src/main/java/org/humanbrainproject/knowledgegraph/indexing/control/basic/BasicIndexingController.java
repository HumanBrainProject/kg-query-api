package org.humanbrainproject.knowledgegraph.indexing.control.basic;

import org.humanbrainproject.knowledgegraph.commons.authorization.entity.OidcAccessToken;
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
    public TodoList insert(QualifiedIndexingMessage message, TodoList todoList, OidcAccessToken oidcAccessToken){
        Vertex vertex = messageProcessor.createVertexStructure(message);
        InsertTodoItem insertTodoItem = new InsertTodoItem(vertex, indexingProvider.getConnection(TargetDatabase.DEFAULT));
        todoList.addTodoItem(insertTodoItem);
        return todoList;
    }

    @Override
    public TodoList update(QualifiedIndexingMessage message, TodoList todoList, OidcAccessToken oidcAccessToken){
        //TODO transfer information about creation / previous authors to new message?
        insert(message, todoList, oidcAccessToken);
        return todoList;
    }

    @Override
    public TodoList delete(NexusInstanceReference reference, TodoList todoList, OidcAccessToken oidcAccessToken) {
        todoList.addTodoItem(new DeleteTodoItem(reference,  indexingProvider.getConnection(TargetDatabase.DEFAULT)));
        return todoList;
    }

    @Override
    public void clear(OidcAccessToken oidcAccessToken) {
        indexingProvider.getConnection(TargetDatabase.DEFAULT).clearData();
    }
}
