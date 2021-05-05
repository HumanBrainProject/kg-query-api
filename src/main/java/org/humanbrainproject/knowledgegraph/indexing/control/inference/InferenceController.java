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

package org.humanbrainproject.knowledgegraph.indexing.control.inference;

import org.humanbrainproject.knowledgegraph.annotations.ToBeTested;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.entity.JsonPath;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.entity.SubSpace;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.entity.Vertex;
import org.humanbrainproject.knowledgegraph.commons.vocabulary.HBPVocabulary;
import org.humanbrainproject.knowledgegraph.indexing.control.IndexingController;
import org.humanbrainproject.knowledgegraph.indexing.control.MessageProcessor;
import org.humanbrainproject.knowledgegraph.indexing.control.nexusToArango.NexusToArangoIndexingProvider;
import org.humanbrainproject.knowledgegraph.indexing.entity.*;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusInstanceReference;
import org.humanbrainproject.knowledgegraph.indexing.entity.todo.DeleteTodoItem;
import org.humanbrainproject.knowledgegraph.indexing.entity.todo.InsertOrUpdateInPrimaryStoreTodoItem;
import org.humanbrainproject.knowledgegraph.indexing.entity.todo.InsertTodoItem;
import org.humanbrainproject.knowledgegraph.indexing.entity.todo.TodoList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * The inference controller applies the registered inference strategies and registers the created / updated / deleted instances into the inferred database.
 */
@Component
@ToBeTested
public class InferenceController implements IndexingController {

    private final static List<JsonPath> EDGE_BLACKLIST_FOR_INFERENCE = Arrays.asList(new JsonPath(HBPVocabulary.INFERENCE_OF), new JsonPath(HBPVocabulary.INFERENCE_EXTENDS), new JsonPath(HBPVocabulary.PROVENANCE_FIELD_UPDATES));

    @Autowired
    MessageProcessor messageProcessor;

    @Autowired
    NexusToArangoIndexingProvider indexingProvider;

    private Set<InferenceStrategy> strategies = Collections.synchronizedSet(new HashSet<>());

    @Override
    public TodoList insert(QualifiedIndexingMessage message, TodoList todoList) {
        if(message.getOriginalMessage().getInstanceReference().getSubspace() != SubSpace.SUGGESTION ) {
            if (message.isOfType(HBPVocabulary.INFERENCE_TYPE)) {
                insertVertexStructure(message, todoList);
            } else {
                Set<Vertex> documents = new HashSet<>();
                for (InferenceStrategy strategy : strategies) {
                    strategy.infer(message, documents);
                }
                if (documents.isEmpty()) {
                    insertVertexStructure(message, todoList);
                } else {
                    documents.forEach(doc -> {
                        todoList.addTodoItem(new InsertOrUpdateInPrimaryStoreTodoItem(doc));
                    });
                }
            }
        }
        return todoList;
    }

    private void insertVertexStructure(QualifiedIndexingMessage message, TodoList todoList) {
        Vertex vertexStructure = messageProcessor.createVertexStructure(message);
        vertexStructure = indexingProvider.mapToOriginalSpace(vertexStructure, message.getOriginalId());
        InsertTodoItem insertTodoItem = new InsertTodoItem(vertexStructure, indexingProvider.getConnection(TargetDatabase.INFERRED));
        insertTodoItem.getBlacklist().addAll(EDGE_BLACKLIST_FOR_INFERENCE);
        todoList.addTodoItem(insertTodoItem);
    }

    @Override
    public TodoList update(QualifiedIndexingMessage message, TodoList todoList) {
        insert(message, todoList);
        return todoList;
    }

    @Override
    public TodoList delete(NexusInstanceReference reference, TodoList todoList) {
        NexusInstanceReference originalIdInMainSpace = indexingProvider.findOriginalId(reference).toSubSpace(SubSpace.MAIN);
        todoList.addTodoItem(new DeleteTodoItem(originalIdInMainSpace, indexingProvider.getConnection(TargetDatabase.INFERRED)));
        return todoList;
    }

    @Override
    public void clear() {
        indexingProvider.getConnection(TargetDatabase.INFERRED).clearData();
    }

    void addInferenceStrategy(InferenceStrategy strategy) {
        strategies.add(strategy);
    }

}
