package org.humanbrainproject.knowledgegraph.boundary.graph;

import org.humanbrainproject.knowledgegraph.control.arango.ArangoDriver;
import org.humanbrainproject.knowledgegraph.control.arango.ArangoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Component
public class ArangoGraph {

    @Autowired
    @Qualifier("default")
    ArangoDriver arango;

    @Autowired
    ArangoRepository arangoUploader;

    public List<Map> getGraph(String rootVertex, Integer step) throws IOException {
        return arangoUploader.inDepthGraph(rootVertex, step, arango);
    }
}
