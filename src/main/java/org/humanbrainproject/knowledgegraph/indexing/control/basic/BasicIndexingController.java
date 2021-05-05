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

package org.humanbrainproject.knowledgegraph.indexing.control.basic;

import org.humanbrainproject.knowledgegraph.annotations.ToBeTested;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.entity.Vertex;
import org.humanbrainproject.knowledgegraph.indexing.control.IndexingController;
import org.humanbrainproject.knowledgegraph.indexing.control.MessageProcessor;
import org.humanbrainproject.knowledgegraph.indexing.control.nexusToArango.NexusToArangoIndexingProvider;
import org.humanbrainproject.knowledgegraph.indexing.entity.*;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusInstanceReference;
import org.humanbrainproject.knowledgegraph.indexing.entity.todo.DeleteTodoItem;
import org.humanbrainproject.knowledgegraph.indexing.entity.todo.InsertTodoItem;
import org.humanbrainproject.knowledgegraph.indexing.entity.todo.TodoList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * The basic indexing controller ensures the insertion of the incoming message into the native / original database
 */
@ToBeTested
@Component
public class BasicIndexingController implements IndexingController {

    @Autowired
    MessageProcessor messageProcessor;

    @Autowired
    NexusToArangoIndexingProvider indexingProvider;


    @Override
    public TodoList insert(QualifiedIndexingMessage message, TodoList todoList){
        Vertex vertex = messageProcessor.createVertexStructure(message);
        InsertTodoItem insertTodoItem = new InsertTodoItem(vertex, indexingProvider.getConnection(TargetDatabase.NATIVE));
        todoList.addTodoItem(insertTodoItem);
        return todoList;
    }

    @Override
    public TodoList update(QualifiedIndexingMessage message, TodoList todoList){
        //TODO transfer information about creation / previous authors to new message?
        insert(message, todoList);
        return todoList;
    }

    @Override
    public TodoList delete(NexusInstanceReference reference, TodoList todoList) {
        todoList.addTodoItem(new DeleteTodoItem(reference,  indexingProvider.getConnection(TargetDatabase.NATIVE)));
        return todoList;
    }

    @Override
    public void clear() {
        indexingProvider.getConnection(TargetDatabase.NATIVE).clearData();
    }
}
