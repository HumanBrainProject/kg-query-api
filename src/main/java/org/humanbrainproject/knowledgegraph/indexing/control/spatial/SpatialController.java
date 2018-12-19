package org.humanbrainproject.knowledgegraph.indexing.control.spatial;

import org.humanbrainproject.knowledgegraph.commons.authorization.entity.Credential;
import org.humanbrainproject.knowledgegraph.indexing.control.IndexingController;
import org.humanbrainproject.knowledgegraph.indexing.control.MessageProcessor;
import org.humanbrainproject.knowledgegraph.indexing.control.nexusToArango.NexusToArangoIndexingProvider;
import org.humanbrainproject.knowledgegraph.indexing.entity.QualifiedIndexingMessage;
import org.humanbrainproject.knowledgegraph.indexing.entity.TodoList;
import org.humanbrainproject.knowledgegraph.indexing.entity.knownSemantics.SpatialAnchoring;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusInstanceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SpatialController implements IndexingController {


    private Logger logger = LoggerFactory.getLogger(SpatialController.class);
    @Autowired
    MessageProcessor messageProcessor;

    @Autowired
    NexusToArangoIndexingProvider indexingProvider;

    @Override
    public TodoList insert(QualifiedIndexingMessage message, TodoList todoList, Credential credential) {
        SpatialAnchoring spatial = new SpatialAnchoring(message);
        if (spatial.isInstance()) {
            logger.info("Found spatial anchoring insert - trigger indexing in Solr");


        }
        return todoList;
    }

    @Override
    public TodoList update(QualifiedIndexingMessage message, TodoList todoList, Credential credential) {
        SpatialAnchoring spatial = new SpatialAnchoring(message);
        if (spatial.isInstance()) {
            logger.info("Found spatial anchoring update - trigger indexing in Solr");

        }
        return todoList;
    }

    @Override
    public TodoList delete(NexusInstanceReference instanceToBeRemoved, TodoList todoList, Credential credential) {
        //TODO check if instance is registered in spatial search - if so, remove it.
        return todoList;
    }

    @Override
    public void clear(Credential credential) {
        //TODO clear spatial search index
    }

}
