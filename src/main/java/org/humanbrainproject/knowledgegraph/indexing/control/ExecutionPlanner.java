package org.humanbrainproject.knowledgegraph.indexing.control;


import org.humanbrainproject.knowledgegraph.commons.propertyGraph.control.DatabaseConnection;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.entity.*;
import org.humanbrainproject.knowledgegraph.indexing.entity.DeleteTodoItem;
import org.humanbrainproject.knowledgegraph.indexing.entity.InsertOrUpdateInPrimaryStoreTodoItem;
import org.humanbrainproject.knowledgegraph.indexing.entity.InsertTodoItem;
import org.humanbrainproject.knowledgegraph.indexing.entity.TodoList;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ExecutionPlanner {

    public void insertVerticesAndEdgesWithoutCheck(TodoList todoList, ResolvedVertexStructure vertexStructure, DatabaseConnection<?> databaseConnection) {
        insertVertexOrEdge(todoList, vertexStructure.getMainVertex(), databaseConnection);
        List<Edge> allEdges = vertexStructure.getMainVertex().getAllEdgesByFollowingEmbedded();
        allEdges.forEach(edge -> insertEdge(todoList, edge, databaseConnection));
    }

    public void insertEdge(TodoList todoList, Edge edge, DatabaseConnection<?> databaseConnection) {
        insertVertexOrEdge(todoList, edge, databaseConnection);
        if (edge instanceof EmbeddedEdge) {
            insertVertexOrEdge(todoList, ((EmbeddedEdge) edge).getToVertex(), databaseConnection);
        }
    }

    public void insertVertexWithEmbeddedInstances(TodoList todoList, MainVertex vertex, DatabaseConnection<?> databaseConnection, List<String> edgeBlacklist) {
        insertVertexOrEdge(todoList, vertex, databaseConnection);
        List<Edge> edges = edgeBlacklist != null ? vertex.getEdgesByFollowingEmbedded(edgeBlacklist) : vertex.getAllEdgesByFollowingEmbedded();
        for (Edge edge : edges) {
            insertEdge(todoList, edge, databaseConnection);
        }
    }

    public void insertVertexOrEdge(TodoList todoList, VertexOrEdge vertexOrEdge, DatabaseConnection<?> databaseConnection) {
        todoList.addTodoItem(new InsertTodoItem(vertexOrEdge, databaseConnection));
    }

    public void deleteVertexOrEdge(TodoList todoList, VertexOrEdgeReference vertexOrEdgeReference, DatabaseConnection<?> databaseConnection) {
        todoList.addTodoItem(new DeleteTodoItem(vertexOrEdgeReference, databaseConnection));
    }

    public void insertVertexOrEdgeInPrimaryStore(TodoList todoList, MainVertex mainVertex) {
        todoList.addTodoItem(new InsertOrUpdateInPrimaryStoreTodoItem(mainVertex));
    }

    public void deleteVerticesAndEmbeddedEdges(TodoList todoList, MainVertex vertex, DatabaseConnection<?> databaseConnection) {
        deleteVertexOrEdge(todoList, vertex, databaseConnection);
        List<Edge> edges = vertex.getEdges();
        for (Edge edge : edges) {
            deleteVertexOrEdge(todoList, edge, databaseConnection);
            if (edge instanceof EmbeddedEdge) {
                Vertex embeddedVertex = ((EmbeddedEdge) edge).getToVertex();
                deleteVertexOrEdge(todoList, embeddedVertex, databaseConnection);
            }
        }
    }


}
