/*
 * Copyright 2018 - 2021 Swiss Federal Institute of Technology Lausanne (EPFL)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * This open source software code was developed in part or in whole in the
 * Human Brain Project, funded from the European Union's Horizon 2020
 * Framework Programme for Research and Innovation under
 * Specific Grant Agreements No. 720270, No. 785907, and No. 945539
 * (Human Brain Project SGA1, SGA2 and SGA3).
 *
 */

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
