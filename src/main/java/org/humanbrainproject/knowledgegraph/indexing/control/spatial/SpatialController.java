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
            solr.delete(ArangoDocumentReference.fromNexusInstance(spatial.getLocatedInstance()).getId(), spatial.getReferenceSpace());
            insert(message, todoList);

        }
        return todoList;
    }

    @Override
    public TodoList delete(NexusInstanceReference instanceToBeRemoved, TodoList todoList) {
        return todoList;
    }

    @Override
    public void clear() {
        //TODO clear spatial search index
    }

}
