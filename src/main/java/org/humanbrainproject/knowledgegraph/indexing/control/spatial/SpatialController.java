package org.humanbrainproject.knowledgegraph.indexing.control.spatial;

import org.apache.solr.client.solrj.SolrServerException;
import org.humanbrainproject.knowledgegraph.annotations.ToBeTested;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoDocumentReference;
import org.humanbrainproject.knowledgegraph.commons.solr.Solr;
import org.humanbrainproject.knowledgegraph.indexing.control.IndexingController;
import org.humanbrainproject.knowledgegraph.indexing.control.MessageProcessor;
import org.humanbrainproject.knowledgegraph.indexing.control.nexusToArango.NexusToArangoIndexingProvider;
import org.humanbrainproject.knowledgegraph.indexing.control.spatial.rasterizer.TwoDimensionRasterizer;
import org.humanbrainproject.knowledgegraph.indexing.entity.QualifiedIndexingMessage;
import org.humanbrainproject.knowledgegraph.indexing.entity.knownSemantics.SpatialAnchoring;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusInstanceReference;
import org.humanbrainproject.knowledgegraph.indexing.entity.todo.TodoList;
import org.humanbrainproject.knowledgegraph.query.entity.ThreeDVector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collection;

/**
 * The spatial controller checks, if the incoming message is a known spatial anchoring. If so, it rasterizes the passed
 * strategy and registers the search points into the spatial search index for further detection.
 */
@Component
@ToBeTested
public class SpatialController implements IndexingController {


    private Logger logger = LoggerFactory.getLogger(SpatialController.class);

    @Autowired
    MessageProcessor messageProcessor;

    @Autowired
    NexusToArangoIndexingProvider indexingProvider;

    @Autowired
    Solr solr;

    @Override
    public TodoList insert(QualifiedIndexingMessage message, TodoList todoList) {
        SpatialAnchoring spatial = new SpatialAnchoring(message);
        if (spatial.isInstance()) {
            logger.info("Found spatial anchoring insert - trigger indexing in Solr");
            Collection<ThreeDVector> points;
            if(spatial.getFormat().isRasterize()) {
                points = new TwoDimensionRasterizer(spatial.getTransformation()).raster();
            }
            else{
                points = spatial.getCoordinatesForPointClouds();
            }
            try{
                solr.registerPoints(ArangoDocumentReference.fromNexusInstance(spatial.getLocatedInstance()).getId(), spatial.getReferenceSpace(), points);
            }
            catch (IOException | SolrServerException e){
                throw new RuntimeException(e);
            }
        }
        return todoList;
    }

    @Override
    public TodoList update(QualifiedIndexingMessage message, TodoList todoList) {
        SpatialAnchoring spatial = new SpatialAnchoring(message);
        if (spatial.isInstance()) {
            logger.info("Found spatial anchoring update - trigger indexing in Solr");

        }
        return todoList;
    }

    @Override
    public TodoList delete(NexusInstanceReference instanceToBeRemoved, TodoList todoList) {
        //TODO check if instance is registered in spatial search - if so, remove it.
        return todoList;
    }

    @Override
    public void clear() {
        //TODO clear spatial search index
    }

}
