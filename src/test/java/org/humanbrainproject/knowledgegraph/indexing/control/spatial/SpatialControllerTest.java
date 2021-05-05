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

package org.humanbrainproject.knowledgegraph.indexing.control.spatial;

import org.humanbrainproject.knowledgegraph.commons.solr.Solr;
import org.humanbrainproject.knowledgegraph.indexing.entity.QualifiedIndexingMessage;
import org.humanbrainproject.knowledgegraph.indexing.entity.todo.TodoList;
import org.humanbrainproject.knowledgegraph.testFactory.TestObjectFactory;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

@Ignore
public class SpatialControllerTest {

    SpatialController spatialController;

    @Before
    public void setup(){
        this.spatialController = new SpatialController();
        this.spatialController.messageProcessor = TestObjectFactory.mockedMessageProcessor();
        this.spatialController.indexingProvider = TestObjectFactory.mockedIndexingProvider();
        this. spatialController.solr = Mockito.mock(Solr.class);
    }


    @Test
    public void insert() {
        QualifiedIndexingMessage indexingMessage = TestObjectFactory.createSpatialAnchoringQualifiedIndexingMessage();
        TodoList todoList = new TodoList();
        todoList = this.spatialController.insert(indexingMessage, todoList);
    }
}