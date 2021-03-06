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

package org.humanbrainproject.knowledgegraph.indexing.boundary;

import org.humanbrainproject.knowledgegraph.annotations.ToBeTested;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.control.DatabaseTransaction;
import org.humanbrainproject.knowledgegraph.indexing.control.IndexingController;
import org.humanbrainproject.knowledgegraph.indexing.control.MessageProcessor;
import org.humanbrainproject.knowledgegraph.indexing.control.basic.BasicIndexingController;
import org.humanbrainproject.knowledgegraph.indexing.control.inference.InferenceController;
import org.humanbrainproject.knowledgegraph.indexing.control.nexusToArango.RelevanceChecker;
import org.humanbrainproject.knowledgegraph.indexing.control.releasing.ReleasingController;
import org.humanbrainproject.knowledgegraph.indexing.control.spatial.SpatialController;
import org.humanbrainproject.knowledgegraph.indexing.entity.IndexingMessage;
import org.humanbrainproject.knowledgegraph.indexing.entity.QualifiedIndexingMessage;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusInstanceReference;
import org.humanbrainproject.knowledgegraph.indexing.entity.todo.TodoList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
@ToBeTested(integrationTestRequired = true, systemTestRequired = true)
public class GraphIndexing {

    @Autowired
    BasicIndexingController defaultIndexingController;

    @Autowired
    ReleasingController releasingController;

    @Autowired
    InferenceController inferenceController;

    @Autowired
    SpatialController spatialController;

    @Autowired
    MessageProcessor messageProcessor;

    @Autowired
    DatabaseTransaction transaction;

    @Autowired
    RelevanceChecker relevanceChecker;


    private Logger logger = LoggerFactory.getLogger(GraphIndexing.class);

    private List<IndexingController> getIndexingControllers() {
        return Arrays.asList(defaultIndexingController, releasingController, inferenceController, spatialController);
    }

    public TodoList insert(IndexingMessage message) {
        //Pre-process
        QualifiedIndexingMessage qualifiedSpec = messageProcessor.qualify(message);
        boolean messageRelevant = relevanceChecker.isMessageRelevant(qualifiedSpec);
        TodoList todoList = new TodoList();
        if (messageRelevant) {
            //Gather execution plan
            for (IndexingController indexingController : getIndexingControllers()) {
                indexingController.insert(qualifiedSpec, todoList);
            }
            //Execute
            transaction.execute(todoList);
        } else {
            logger.info("Skipping indexing of instance " + message.getInstanceReference() + " because we have indexed a later revision already");
        }
        return todoList;
    }

    public TodoList update(IndexingMessage message) {
        //Pre-process
        QualifiedIndexingMessage qualifiedSpec = messageProcessor.qualify(message);
        boolean messageRelevant = relevanceChecker.isMessageRelevant(qualifiedSpec);

        TodoList todoList = new TodoList();
        if (messageRelevant) {
            //Gather execution plan
            for (IndexingController indexingController : getIndexingControllers()) {
                indexingController.update(qualifiedSpec, todoList);
            }

            //Execute
            transaction.execute(todoList);
        } else {
            logger.info("Skipping indexing of instance " + message.getInstanceReference() + " because we have indexed a later revision already");
        }
        return todoList;
    }


    public TodoList delete(NexusInstanceReference reference) {
        //Gather execution plan
        TodoList todoList = new TodoList();
        for (IndexingController indexingController : getIndexingControllers()) {
            indexingController.delete(reference, todoList);
        }
        //Execute
        transaction.execute(todoList);
        return todoList;
    }

    public void clearGraph() {
        for (IndexingController indexingController : getIndexingControllers()) {
            indexingController.clear();
        }
    }

}
