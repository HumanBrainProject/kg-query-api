package org.humanbrainproject.knowledgegraph.indexing.control.basic;

import org.humanbrainproject.knowledgegraph.commons.propertyGraph.entity.ResolvedVertexStructure;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.entity.VertexOrEdgeReference;
import org.humanbrainproject.knowledgegraph.indexing.control.ExecutionPlanner;
import org.humanbrainproject.knowledgegraph.indexing.control.IndexingController;
import org.humanbrainproject.knowledgegraph.indexing.control.MessageProcessor;
import org.humanbrainproject.knowledgegraph.indexing.control.nexusToArango.NexusToArangoIndexingProvider;
import org.humanbrainproject.knowledgegraph.indexing.entity.QualifiedIndexingMessage;
import org.humanbrainproject.knowledgegraph.indexing.entity.TargetDatabase;
import org.humanbrainproject.knowledgegraph.indexing.entity.TodoList;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusInstanceReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class BasicIndexingController implements IndexingController {

    @Autowired
    MessageProcessor messageProcessor;

    @Autowired
    ExecutionPlanner executionPlanner;

    @Autowired
    NexusToArangoIndexingProvider indexingProvider;


    @Override
    public TodoList insert(QualifiedIndexingMessage message, TodoList todoList){
        ResolvedVertexStructure vertexStructure = messageProcessor.createVertexStructure(message);
        executionPlanner.insertVerticesAndEdgesWithoutCheck(todoList, vertexStructure, indexingProvider.getConnection(TargetDatabase.DEFAULT));
        return todoList;
    }

    @Override
    public TodoList update(QualifiedIndexingMessage message, TodoList todoList){
        delete(message.getOriginalMessage().getInstanceReference(), todoList, message.getOriginalMessage().getTimestamp(), message.getOriginalMessage().getUserId());
        insert(message, todoList);
        return todoList;
    }

    @Override
    public TodoList delete(NexusInstanceReference reference, TodoList todoList, String timestamp, String userId) {
        Set<VertexOrEdgeReference> vertexOrEdgeReferences = indexingProvider.getVertexOrEdgeReferences(reference, TargetDatabase.DEFAULT);
        for (VertexOrEdgeReference vertexOrEdgeReference : vertexOrEdgeReferences) {
            executionPlanner.deleteVertexOrEdge(todoList, vertexOrEdgeReference, indexingProvider.getConnection(TargetDatabase.DEFAULT));
        }
        return todoList;
    }

    @Override
    public void clear() {
        indexingProvider.getConnection(TargetDatabase.DEFAULT).clearData();
    }
}
