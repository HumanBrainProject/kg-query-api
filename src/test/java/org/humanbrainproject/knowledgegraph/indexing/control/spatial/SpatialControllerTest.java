package org.humanbrainproject.knowledgegraph.indexing.control.spatial;

import org.humanbrainproject.knowledgegraph.indexing.entity.QualifiedIndexingMessage;
import org.humanbrainproject.knowledgegraph.indexing.entity.TodoList;
import org.humanbrainproject.knowledgegraph.testFactory.TestObjectFactory;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

@Ignore
public class SpatialControllerTest {

    SpatialController spatialController;

    @Before
    public void setup(){
        this.spatialController = new SpatialController();
        this.spatialController.messageProcessor = TestObjectFactory.mockedMessageProcessor();
        this.spatialController.indexingProvider = TestObjectFactory.mockedIndexingProvider();
    }


    @Test
    public void insert() {
        QualifiedIndexingMessage indexingMessage = TestObjectFactory.createSpatialAnchoringQualifiedIndexingMessage();
        TodoList todoList = new TodoList();
        todoList = this.spatialController.insert(indexingMessage, todoList, TestObjectFactory.credential());
    }
}