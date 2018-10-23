package org.humanbrainproject.knowledgegraph.indexing.control.basic;

import org.humanbrainproject.knowledgegraph.indexing.control.ExecutionPlanner;
import org.humanbrainproject.knowledgegraph.indexing.control.IndexingController;
import org.humanbrainproject.knowledgegraph.indexing.control.IndexingProvider;
import org.humanbrainproject.knowledgegraph.indexing.control.MessageProcessor;
import org.humanbrainproject.knowledgegraph.indexing.entity.*;
import org.humanbrainproject.knowledgegraph.propertyGraph.entity.Edge;
import org.humanbrainproject.knowledgegraph.propertyGraph.entity.MainVertex;
import org.humanbrainproject.knowledgegraph.propertyGraph.entity.ResolvedVertexStructure;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
public class BasicIndexingController implements IndexingController {

    @Autowired
    MessageProcessor messageProcessor;

    @Autowired
    ExecutionPlanner executionPlanner;

    @Autowired
    IndexingProvider indexingProvider;


    @Override
    public void insert(QualifiedIndexingMessage message, TodoList todoList){
        ResolvedVertexStructure vertexStructure = messageProcessor.createVertexStructure(message);
        executionPlanner.insertVerticesAndEdgesWithoutCheck(todoList, vertexStructure, indexingProvider.getConnection(TargetDatabase.DEFAULT));
    }

    @Override
    public void update(QualifiedIndexingMessage message, TodoList todoList){
        MainVertex currentStateInDB = indexingProvider.getVertexStructureById(message.getOriginalMessage().getInstanceReference(), TargetDatabase.DEFAULT);
        if(currentStateInDB==null){
            //There is no current state in the database - this means, we're going to insert the message instead.
            insert(message, todoList);
        }
        else {
            ResolvedVertexStructure newVertex = messageProcessor.createVertexStructure(message);
            //For sure, we'll update the main vertex
            executionPlanner.addVertexOrEdgeToTodoList(todoList, newVertex.getMainVertex(),indexingProvider.getConnection(TargetDatabase.DEFAULT), TodoItem.Action.UPDATE);
            List<Edge> allOriginalEdges = currentStateInDB.getAllEdgesByFollowingEmbedded();
            List<Edge> allNewEdges = newVertex.getMainVertex().getAllEdgesByFollowingEmbedded();

            List<Edge> toBeRemoved = new ArrayList<>();
            Collections.copy(allOriginalEdges, toBeRemoved);
            toBeRemoved.removeAll(allNewEdges);
            toBeRemoved.forEach(edgeToBeRemoved -> executionPlanner.addEdgeToTodoList(todoList, edgeToBeRemoved, indexingProvider.getConnection(TargetDatabase.DEFAULT), TodoItem.Action.DELETE));

            List<Edge> toBeCreated = new ArrayList<>();
            Collections.copy(allNewEdges, toBeCreated);
            toBeCreated.removeAll(allOriginalEdges);
            toBeCreated.forEach(edgeToBeCreated -> executionPlanner.addEdgeToTodoList(todoList, edgeToBeCreated, indexingProvider.getConnection(TargetDatabase.DEFAULT), TodoItem.Action.INSERT));

            List<Edge> toBeUpdated = new ArrayList<>();
            Collections.copy(allNewEdges, toBeUpdated);
            toBeUpdated.retainAll(allOriginalEdges);
            toBeUpdated.forEach(edgeToBeUpdated -> executionPlanner.addEdgeToTodoList(todoList, edgeToBeUpdated, indexingProvider.getConnection(TargetDatabase.DEFAULT), TodoItem.Action.UPDATE));
        }
    }

    @Override
    public void delete(InstanceReference reference, TodoList todoList) {
        MainVertex vertexStructureById = indexingProvider.getVertexStructureById(reference, TargetDatabase.DEFAULT);
        executionPlanner.addVertexWithEmbeddedInstancesToTodoList(todoList, vertexStructureById, indexingProvider.getConnection(TargetDatabase.DEFAULT), TodoItem.Action.DELETE);
    }

    @Override
    public void clear() {
        indexingProvider.getConnection(TargetDatabase.DEFAULT).clearData();
    }
}
