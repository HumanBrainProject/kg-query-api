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
