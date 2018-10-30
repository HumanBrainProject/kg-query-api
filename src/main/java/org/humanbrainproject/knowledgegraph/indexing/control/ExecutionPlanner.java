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

    public <T> void insertVerticesAndEdgesWithoutCheck(TodoList<T> todoList, ResolvedVertexStructure vertexStructure, DatabaseConnection<T> databaseConnection){
        insertVertexOrEdge(todoList, vertexStructure.getMainVertex(), databaseConnection);
        List<Edge> allEdges = vertexStructure.getMainVertex().getAllEdgesByFollowingEmbedded();
        allEdges.forEach(edge -> insertEdge(todoList, edge, databaseConnection));
    }

    public <T> void insertEdge(TodoList<T> todoList, Edge edge, DatabaseConnection<T> databaseConnection){
        insertVertexOrEdge(todoList, edge, databaseConnection);
        if(edge instanceof EmbeddedEdge){
            insertVertexOrEdge(todoList, ((EmbeddedEdge)edge).getToVertex(), databaseConnection);
        }
    }

    public <T> void insertVertexWithEmbeddedInstances(TodoList<T> todoList, MainVertex vertex, DatabaseConnection<T> databaseConnection){
        insertVertexOrEdge(todoList, vertex, databaseConnection);
        for (Edge edge : vertex.getAllEdgesByFollowingEmbedded()) {
           insertEdge(todoList, edge, databaseConnection);
        }
    }

    public <T> void insertVertexOrEdge(TodoList<T> todoList, VertexOrEdge vertexOrEdge, DatabaseConnection<T> databaseConnection){
        todoList.addTodoItem(new InsertTodoItem<>(vertexOrEdge, databaseConnection));
    }

    public <T> void deleteVertexOrEdge(TodoList<T> todoList, VertexOrEdgeReference vertexOrEdgeReference, DatabaseConnection<T> databaseConnection){
        todoList.addTodoItem(new DeleteTodoItem<>(vertexOrEdgeReference, databaseConnection));
    }

    public <T> void insertVertexOrEdgeInPrimaryStore(TodoList<T> todoList, MainVertex mainVertex){
        todoList.addTodoItem(new InsertOrUpdateInPrimaryStoreTodoItem(mainVertex));
    }

    public <T> void deleteVerticesAndEmbeddedEdges(TodoList<T> todoList, MainVertex vertex, DatabaseConnection<T> databaseConnection){
        deleteVertexOrEdge(todoList, vertex, databaseConnection);
        List<Edge> edges = vertex.getEdges();
        for (Edge edge : edges) {
            deleteVertexOrEdge(todoList, edge, databaseConnection);
            if(edge instanceof EmbeddedEdge){
                Vertex embeddedVertex = ((EmbeddedEdge) edge).getToVertex();
                deleteVertexOrEdge(todoList, embeddedVertex, databaseConnection);
            }
        }
    }



}
