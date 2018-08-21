package org.humanbrainproject.knowledgegraph.boundary.indexing;

import org.humanbrainproject.knowledgegraph.control.arango.ArangoDriver;
import org.humanbrainproject.knowledgegraph.control.arango.query.ArangoSpecificationQuery;
import org.humanbrainproject.knowledgegraph.control.arango.ArangoRepository;
import org.humanbrainproject.knowledgegraph.control.jsonld.JsonLdStandardization;
import org.humanbrainproject.knowledgegraph.control.releasing.ReleasingController;
import org.humanbrainproject.knowledgegraph.control.specification.SpecificationInterpreter;
import org.humanbrainproject.knowledgegraph.entity.jsonld.JsonLdEdge;
import org.humanbrainproject.knowledgegraph.entity.jsonld.JsonLdVertex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.configurationprocessor.json.JSONException;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class ArangoIndexing extends GraphIndexing {

    @Autowired
    @Qualifier("default")
    ArangoDriver defaultDB;

    @Autowired
    @Qualifier("released")
    ArangoDriver releasedDB;

    @Autowired
    ArangoRepository repository;

    @Autowired
    SpecificationInterpreter specInterpreter;

    @Autowired
    ArangoSpecificationQuery specificationQuery;

    @Autowired
    JsonLdStandardization standardization;

    @Autowired
    ReleasingController releasingController;

    public String getById(String entityName, String id){
        return repository.getByKey(entityName, id, String.class, defaultDB);
    }

    @Override
    void transactionalJsonLdInsertion(List<JsonLdVertex> jsonLdVertices) throws JSONException {
        repository.uploadToPropertyGraph(jsonLdVertices, defaultDB);
        if(releasingController.isRelevantForReleasing(jsonLdVertices)){
            //Upload to released database
            releasingController.releaseVertices(jsonLdVertices, defaultDB, releasedDB);
        }
    }

    @Override
    void transactionalJsonLdUpdate(List<JsonLdVertex> jsonLdVertices) throws JSONException {
        boolean relevantForReleasing = releasingController.isRelevantForReleasing(jsonLdVertices);
        List<List<String>> verticesToBeUnreleased = Collections.emptyList();
        if(relevantForReleasing){
            verticesToBeUnreleased = jsonLdVertices.stream().map(v -> {
                List<JsonLdEdge> edgesToBeRemoved = repository.getEdgesToBeRemoved(v, defaultDB);
                return edgesToBeRemoved.stream().map(e -> repository.getTargetVertexId(e, defaultDB)).collect(Collectors.toList());
            }).collect(Collectors.toList());
        }
        repository.uploadToPropertyGraph(jsonLdVertices, defaultDB);
        if(relevantForReleasing){
            //Upload to released database
            releasingController.releaseVertices(jsonLdVertices, defaultDB, releasedDB);
            verticesToBeUnreleased.forEach(vertices -> vertices.forEach(v -> releasingController.unreleaseInstance(v, releasedDB)));
        }
    }

    @Override
    void transactionalJsonLdDeletion(String entityName, String key, Integer rootRev) {
        Map instance = repository.getByKey(entityName, key, Map.class, defaultDB);
        if(instance!=null) {
            repository.deleteVertex(entityName, key, defaultDB);
            if (releasingController.isRelevantForReleasing(instance)) {
                releasingController.unreleaseVertices(instance, releasedDB);
            }
            repository.deleteVertex(entityName, key, defaultDB);
        }
    }

    @Override
    public void clearGraph() {
        repository.clearDatabase(defaultDB.getOrCreateDB());
        if(releasedDB!=null) {
            repository.clearDatabase(releasedDB.getOrCreateDB());
        }
    }
}
