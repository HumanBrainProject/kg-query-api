package org.humanbrainproject.knowledgegraph.indexing.entity.todo;

import org.humanbrainproject.knowledgegraph.annotations.NoTests;

import java.util.ArrayList;
import java.util.List;

@NoTests(NoTests.TRIVIAL)
public class TodoList {

    private List<DeleteTodoItem> deleteTodoItems = new ArrayList<>();
    private List<InsertTodoItem> insertTodoItems = new ArrayList<>();
    private List<InsertOrUpdateInPrimaryStoreTodoItem> insertOrUpdateInPrimaryStoreTodoItems = new ArrayList<>();


    private final List<TodoItem> todoItems = new ArrayList<>();

    public void addTodoItem(DeleteTodoItem deleteTodoItem){
        this.deleteTodoItems.add(deleteTodoItem);
    }

    public void addTodoItem(InsertTodoItem insertTodoItem){
        this.insertTodoItems.add(insertTodoItem);
    }


    public void addTodoItem(InsertOrUpdateInPrimaryStoreTodoItem insertOrUpdateInPrimaryStoreTodoItem){
        this.insertOrUpdateInPrimaryStoreTodoItems.add(insertOrUpdateInPrimaryStoreTodoItem);
    }

    public List<DeleteTodoItem> getDeleteTodoItems() {
        return deleteTodoItems;
    }

    public List<InsertTodoItem> getInsertTodoItems() {
        return insertTodoItems;
    }

    public List<InsertOrUpdateInPrimaryStoreTodoItem> getInsertOrUpdateInPrimaryStoreTodoItems() {
        return insertOrUpdateInPrimaryStoreTodoItems;
    }
}
