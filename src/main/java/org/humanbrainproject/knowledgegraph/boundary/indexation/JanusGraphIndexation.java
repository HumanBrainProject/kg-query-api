package org.humanbrainproject.knowledgegraph.boundary.indexation;

import org.humanbrainproject.knowledgegraph.control.janusgraph.JanusGraphDriver;
import org.humanbrainproject.knowledgegraph.control.janusgraph.JanusGraphRepository;
import org.humanbrainproject.knowledgegraph.entity.jsonld.JsonLdVertex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class JanusGraphIndexation {

    @Autowired
    JanusGraphRepository verticesAndEdgesUploader;

    @Autowired
    JanusGraphDriver janusGraphCluster;

    void transactionalJsonLdUpload(List<JsonLdVertex> vertices) {
        verticesAndEdgesUploader.uploadToPropertyGraph(janusGraphCluster.getGraphTraversalSource(), vertices);
    }

    public void clearGraph(){
        verticesAndEdgesUploader.clearGraph(janusGraphCluster.getGraphTraversalSource());
    }
}
