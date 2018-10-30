package org.humanbrainproject.knowledgegraph.indexing.control.basic;

import org.humanbrainproject.knowledgegraph.indexing.control.ExecutionPlanner;
import org.humanbrainproject.knowledgegraph.indexing.control.IndexingProvider;
import org.humanbrainproject.knowledgegraph.indexing.control.MessageProcessor;
import org.humanbrainproject.knowledgegraph.indexing.entity.IndexingMessage;
import org.humanbrainproject.knowledgegraph.indexing.entity.InstanceReference;
import org.humanbrainproject.knowledgegraph.indexing.entity.QualifiedIndexingMessage;
import org.humanbrainproject.knowledgegraph.indexing.entity.TodoList;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusInstanceReference;
import org.humanbrainproject.knowledgegraph.commons.jsonld.control.JsonTransformer;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.LinkedHashMap;
import java.util.Map;

public class BasicIndexingControllerTest {

    BasicIndexingController controller;

    JsonTransformer jsonTransformer;


    @Before
    public void setup(){
        controller = new BasicIndexingController();
        controller.executionPlanner = new ExecutionPlanner();


        controller.messageProcessor = Mockito.mock(MessageProcessor.class);
        controller.indexingProvider = Mockito.mock(IndexingProvider.class);
        jsonTransformer = new JsonTransformer();
    }


    @Test
    public void insert() {
        Map<String, Object> fullyQualified = new LinkedHashMap<>();
        fullyQualified.put("http://test/foo", "foo");
        TodoList todoList = new TodoList();
        InstanceReference instanceReference = new NexusInstanceReference("foo", "core", "bar", "v0.0.1", "fooBar");
        IndexingMessage indexingMessage = new IndexingMessage(instanceReference,  jsonTransformer.getMapAsJson(fullyQualified));
        QualifiedIndexingMessage qualifiedIndexingMessage = new QualifiedIndexingMessage(indexingMessage, fullyQualified);
        controller.insert(qualifiedIndexingMessage, todoList);
        System.out.println(todoList);

    }

    @Test
    public void update() {
    }

    @Test
    public void delete() {
    }
}