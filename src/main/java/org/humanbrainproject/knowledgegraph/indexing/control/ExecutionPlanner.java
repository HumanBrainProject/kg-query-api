package org.humanbrainproject.knowledgegraph.indexing.control;

import org.humanbrainproject.knowledgegraph.propertyGraph.control.DatabaseConnection;
import org.humanbrainproject.knowledgegraph.indexing.entity.TodoItem;
import org.humanbrainproject.knowledgegraph.indexing.entity.TodoList;
import org.humanbrainproject.knowledgegraph.propertyGraph.entity.*;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ExecutionPlanner {

    public TodoList insertVerticesAndEdgesWithoutCheck(TodoList todoList, ResolvedVertexStructure vertexStructure, DatabaseConnection<?> databaseConnection){
        addVertexOrEdgeToTodoList(todoList, vertexStructure.getMainVertex(), databaseConnection, TodoItem.Action.INSERT);
        List<Edge> allEdges = vertexStructure.getMainVertex().getAllEdgesByFollowingEmbedded();
        allEdges.forEach(edge -> addEdgeToTodoList(todoList, edge, databaseConnection, TodoItem.Action.INSERT));
        return todoList;
    }

    public void addEdgeToTodoList(TodoList todoList, Edge edge, DatabaseConnection<?> databaseConnection, TodoItem.Action action){
        addVertexOrEdgeToTodoList(todoList, edge, databaseConnection, action);
        if(edge instanceof EmbeddedEdge){
            addVertexOrEdgeToTodoList(todoList, ((EmbeddedEdge)edge).getToVertex(), databaseConnection, action);
        }
    }

    public void addVertexWithEmbeddedInstancesToTodoList(TodoList todoList, MainVertex vertex, DatabaseConnection<?> databaseConnection, TodoItem.Action action){
        addVertexOrEdgeToTodoList(todoList, vertex, databaseConnection, action);
        for (Edge edge : vertex.getAllEdgesByFollowingEmbedded()) {
            addEdgeToTodoList(todoList, edge, databaseConnection, action);
        }
    }


    public void addVertexOrEdgeToTodoList(TodoList todoList, VertexOrEdge vertexOrEdge, DatabaseConnection<?> databaseConnection, TodoItem.Action action){
        TodoItem todoItem = new TodoItem(vertexOrEdge, databaseConnection, action);
        todoList.addTodoItem(todoItem);
    }

    public TodoList removeVerticesAndEmbeddedEdges(TodoList todoList, MainVertex vertex, DatabaseConnection<?> databaseConnection){
        addVertexOrEdgeToTodoList(todoList, vertex, databaseConnection, TodoItem.Action.DELETE);
        List<Edge> edges = vertex.getEdges();
        for (Edge edge : edges) {
            addVertexOrEdgeToTodoList(todoList, edge, databaseConnection, TodoItem.Action.DELETE);
            if(edge instanceof EmbeddedEdge){
                Vertex embeddedVertex = ((EmbeddedEdge) edge).getToVertex();
                addVertexOrEdgeToTodoList(todoList, embeddedVertex, databaseConnection, TodoItem.Action.DELETE);

            }
        }
        return todoList;
    }



}
