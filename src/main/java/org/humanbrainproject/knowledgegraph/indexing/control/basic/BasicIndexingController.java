package org.humanbrainproject.knowledgegraph.indexing.control.basic;

import org.humanbrainproject.knowledgegraph.commons.propertyGraph.entity.ResolvedVertexStructure;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.entity.VertexOrEdgeReference;
import org.humanbrainproject.knowledgegraph.indexing.control.ExecutionPlanner;
import org.humanbrainproject.knowledgegraph.indexing.control.IndexingController;
import org.humanbrainproject.knowledgegraph.indexing.control.IndexingProvider;
import org.humanbrainproject.knowledgegraph.indexing.control.MessageProcessor;
import org.humanbrainproject.knowledgegraph.indexing.entity.InstanceReference;
import org.humanbrainproject.knowledgegraph.indexing.entity.QualifiedIndexingMessage;
import org.humanbrainproject.knowledgegraph.indexing.entity.TargetDatabase;
import org.humanbrainproject.knowledgegraph.indexing.entity.TodoList;
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
    IndexingProvider indexingProvider;


    @Override
    public <T> TodoList<T> insert(QualifiedIndexingMessage message, TodoList<T> todoList){
        ResolvedVertexStructure vertexStructure = messageProcessor.createVertexStructure(message);
        executionPlanner.insertVerticesAndEdgesWithoutCheck(todoList, vertexStructure, indexingProvider.getConnection(TargetDatabase.DEFAULT));
        return todoList;
    }

    @Override
    public <T> TodoList<T> update(QualifiedIndexingMessage message, TodoList<T> todoList){
        delete(message.getOriginalMessage().getInstanceReference(), todoList);
        insert(message, todoList);
        return todoList;
    }

    @Override
    public <T> TodoList<T> delete(InstanceReference reference, TodoList<T> todoList) {
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
