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

    public List<Map> getReleaseGraph(String rootVertex) throws IOException {
        return arangoRepository.releaseGraph(rootVertex, arango);
    }

    public void uploadFunction(String name, String function) throws  IOException{
        arangoRepository.uploadFunction(name, function, arango);
    }

    public Collection<AqlFunctionEntity> getAqlFunctions(String namespace) throws IOException{
        return arangoRepository.getAqlFunctions(namespace, arango);
    }
}
