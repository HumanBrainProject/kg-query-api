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

import org.humanbrainproject.knowledgegraph.indexing.entity.QualifiedIndexingMessage;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusInstanceReference;
import org.humanbrainproject.knowledgegraph.indexing.entity.todo.TodoList;
import org.humanbrainproject.knowledgegraph.testFactory.TestObjectFactory;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.LinkedHashMap;
import java.util.Map;

@SpringBootTest
@RunWith(SpringRunner.class)

@Ignore("IntegrationTest")
public class BasicIndexingControllerIntegrationTest {

    @Autowired
    BasicIndexingController controller;


    @Before
    public void setup() {
    }


    @Test
    public void insert() {
        Map<String, Object> fullyQualified = new LinkedHashMap<>();
        fullyQualified.put("http://test/foo", "foo");
        NexusInstanceReference instanceReference = TestObjectFactory.fooInstanceReference();
        QualifiedIndexingMessage qualifiedIndexingMessage = TestObjectFactory.createQualifiedIndexingMessage(instanceReference, fullyQualified);
        TodoList todoList = controller.insert(qualifiedIndexingMessage, new TodoList());
        System.out.println(todoList);

    }

    @Test
    public void update() {
        Map<String, Object> fullyQualified = new LinkedHashMap<>();
        fullyQualified.put("http://test/foo", "foo");
        NexusInstanceReference instanceReference = new NexusInstanceReference("minds", "core", "dataset", "v0.0.4", "0032bda4-50e3-4dc9-ab87-980de4f526a2");
        QualifiedIndexingMessage qualifiedIndexingMessage = TestObjectFactory.createQualifiedIndexingMessage(instanceReference, fullyQualified);
        TodoList todoList = controller.update(qualifiedIndexingMessage, new TodoList());
        System.out.println(todoList);
    }

    @Test
    public void delete() {

    }
}