package org.humanbrainproject.knowledgegraph.indexing.entity;

import java.util.ArrayList;
import java.util.List;

public class TodoList {

    private final List<TodoItem> todoItems = new ArrayList<>();

    public void addTodoItem(TodoItem todoItem){
        this.todoItems.add(todoItem);
    }
}
