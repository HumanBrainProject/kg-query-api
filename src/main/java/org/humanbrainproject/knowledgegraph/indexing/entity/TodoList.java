package org.humanbrainproject.knowledgegraph.indexing.entity;

import java.util.ArrayList;
import java.util.List;

public class TodoList<T> {

    private List<DeleteTodoItem<T>> deleteTodoItems = new ArrayList<>();
    private List<InsertTodoItem<T>> insertTodoItems = new ArrayList<>();
    private List<InsertOrUpdateInPrimaryStoreTodoItem<T>> insertOrUpdateInPrimaryStoreTodoItems = new ArrayList<>();


    private final List<TodoItem> todoItems = new ArrayList<>();

    public void addTodoItem(DeleteTodoItem<T> deleteTodoItem){
        this.deleteTodoItems.add(deleteTodoItem);
    }

    public void addTodoItem(InsertTodoItem<T> insertTodoItem){
        this.insertTodoItems.add(insertTodoItem);
    }


    public void addTodoItem(InsertOrUpdateInPrimaryStoreTodoItem<T> insertOrUpdateInPrimaryStoreTodoItem){
        this.insertOrUpdateInPrimaryStoreTodoItems.add(insertOrUpdateInPrimaryStoreTodoItem);
    }

    public List<DeleteTodoItem<T>> getDeleteTodoItems() {
        return deleteTodoItems;
    }

    public List<InsertTodoItem<T>> getInsertTodoItems() {
        return insertTodoItems;
    }

    public List<InsertOrUpdateInPrimaryStoreTodoItem<T>> getInsertOrUpdateInPrimaryStoreTodoItems() {
        return insertOrUpdateInPrimaryStoreTodoItems;
    }
}
