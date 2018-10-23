package org.humanbrainproject.knowledgegraph.indexing.boundary;

import org.humanbrainproject.knowledgegraph.indexing.control.IndexingController;
import org.humanbrainproject.knowledgegraph.indexing.control.MessageProcessor;
import org.humanbrainproject.knowledgegraph.indexing.control.basic.BasicIndexingController;
import org.humanbrainproject.knowledgegraph.indexing.control.inference.InferenceController;
import org.humanbrainproject.knowledgegraph.indexing.control.releasing.ReleasingController;
import org.humanbrainproject.knowledgegraph.indexing.entity.IndexingMessage;
import org.humanbrainproject.knowledgegraph.indexing.entity.QualifiedIndexingMessage;
import org.humanbrainproject.knowledgegraph.indexing.entity.TodoList;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusInstanceReference;
import org.humanbrainproject.knowledgegraph.propertyGraph.arango.control.ArangoTransaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class GraphIndexing {

    @Autowired
    BasicIndexingController defaultIndexingController;

    @Autowired
    ReleasingController releasingController;

    @Autowired
    InferenceController inferenceController;

    @Autowired
    MessageProcessor messageProcessor;


    private Logger logger = LoggerFactory.getLogger(GraphIndexing.class);

    private List<IndexingController> getIndexingControllers(){
        return Arrays.asList(defaultIndexingController, releasingController, inferenceController);
    }

    public void insert(IndexingMessage message) {
        //Pre-process
        QualifiedIndexingMessage qualifiedSpec = messageProcessor.qualify(message);

        //Gather execution plan
        TodoList todoList = new TodoList();
        for (IndexingController indexingController : getIndexingControllers()) {
            indexingController.insert(qualifiedSpec, todoList);
        }

        //Execute
        new ArangoTransaction().execute(todoList);
    }

    public void update(IndexingMessage message) {
        //Pre-process
        QualifiedIndexingMessage qualifiedSpec = messageProcessor.qualify(message);

        //Gather execution plan
        TodoList todoList = new TodoList();
        for (IndexingController indexingController : getIndexingControllers()) {
            indexingController.insert(qualifiedSpec, todoList);
        }

        //Execute
        new ArangoTransaction().execute(todoList);
    }


    public void delete(NexusInstanceReference reference){

        //Gather execution plan
        TodoList todoList = new TodoList();
        for (IndexingController indexingController : getIndexingControllers()) {
            indexingController.delete(reference, todoList);
        }

        //Execute
        new ArangoTransaction().execute(todoList);
    }

    public void clearGraph() {
        for (IndexingController indexingController : getIndexingControllers()) {
            indexingController.clear();
        }
    }

}
