package org.humanbrainproject.knowledgegraph.boundary.graph;

import com.arangodb.entity.AqlFunctionEntity;
import org.humanbrainproject.knowledgegraph.control.arango.ArangoDriver;
import org.humanbrainproject.knowledgegraph.control.arango.ArangoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class ArangoGraph {

    @Autowired
    @Qualifier("default")
    ArangoDriver arango;

    @Autowired
    ArangoRepository arangoRepository;

    public List<Map> getGraph(String rootVertex, Integer step) throws IOException {
        return arangoRepository.inDepthGraph(rootVertex, step, arango);
    }

    public List<Map> getReleaseGraph(String rootVertex, Optional<Integer> maxDepthOpt) throws IOException {
        Integer maxDepth = maxDepthOpt.orElse(6);
        return arangoRepository.releaseGraph(rootVertex, maxDepth, arango);
    }
    public List<Map> getDocument(String documentID) throws IOException{
        return  arangoRepository.getDocument(documentID, arango);
    }

    public List<Map> getInstanceList(String collection, Integer from, Integer size) throws IOException{
        return arangoRepository.getInstanceList(collection, from, size ,arango);
    }
}
