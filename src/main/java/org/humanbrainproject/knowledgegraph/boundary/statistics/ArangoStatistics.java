package org.humanbrainproject.knowledgegraph.boundary.statistics;

import com.arangodb.entity.CollectionType;
import org.humanbrainproject.knowledgegraph.control.arango.ArangoDriver;
import org.humanbrainproject.knowledgegraph.control.arango.ArangoRepository;
import org.humanbrainproject.knowledgegraph.entity.Tuple;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.stream.Collectors;

@Component
public class ArangoStatistics {

    @Autowired
    ArangoRepository repository;

    @Autowired
    ArangoDriver arangoDriver;

    public Map<String, Object> getCountsPerCollection(){
        return arangoDriver.getOrCreateDB().getCollections().parallelStream().filter(c -> c.getIsSystem() != null && !c.getIsSystem() && c.getType() == CollectionType.DOCUMENT).map(c -> repository.countInstances(c.getName(), arangoDriver)).collect(Collectors.toMap(Tuple::getValue1, Tuple::getValue2));
    }



}
