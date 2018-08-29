package org.humanbrainproject.knowledgegraph.boundary.indexing;

import org.humanbrainproject.knowledgegraph.control.arango.ArangoDriver;
import org.humanbrainproject.knowledgegraph.control.arango.ArangoRepository;
import org.humanbrainproject.knowledgegraph.control.arango.query.ArangoSpecificationQuery;
import org.humanbrainproject.knowledgegraph.control.jsonld.JsonLdStandardization;
import org.humanbrainproject.knowledgegraph.control.releasing.ReleasingController;
import org.humanbrainproject.knowledgegraph.control.spatialSearch.SpatialSearchController;
import org.humanbrainproject.knowledgegraph.control.specification.SpecificationInterpreter;
import org.humanbrainproject.knowledgegraph.entity.jsonld.JsonLdVertex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.configurationprocessor.json.JSONException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

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

    @Autowired
    SpatialSearchController spatialSearchController;


    public String getById(String entityName, String id) {
        return repository.getByKey(entityName, id, String.class, defaultDB);
    }

    @Override
    void transactionalJsonLdInsertion(List<JsonLdVertex> jsonLdVertices) throws JSONException {
        repository.uploadToPropertyGraph(jsonLdVertices, defaultDB);
        if(spatialSearchController.isRelevant(jsonLdVertices)){
            spatialSearchController.index(jsonLdVertices);
        }
        if (releasingController.isRelevantForReleasing(jsonLdVertices)) {
            //Upload to released database
            releasingController.releaseVertices(jsonLdVertices, defaultDB, releasedDB);
        }
    }

    @Override
    void transactionalJsonLdUpdate(List<JsonLdVertex> jsonLdVertices) throws JSONException {
        boolean releasingRelevant = releasingController.isRelevantForReleasing(jsonLdVertices);
        List<List<String>> verticesToBeUnreleased = releasingRelevant? releasingController.findDocumentsToBeUnreleased(jsonLdVertices, defaultDB) : null;
        boolean spatialSearchRelevant = spatialSearchController.isRelevant(jsonLdVertices);
        List<List<String>> elementsToBeRemovedFromSpatialSearch = spatialSearchRelevant ? spatialSearchController.findDocumentsToBeRemovedFromSpatialSearch(jsonLdVertices) : null;
        repository.uploadToPropertyGraph(jsonLdVertices, defaultDB);
        if(spatialSearchRelevant){
            spatialSearchController.index(jsonLdVertices);
        }
        spatialSearchController.remove(elementsToBeRemovedFromSpatialSearch);
        if(releasingRelevant){
            releasingController.releaseVertices(jsonLdVertices, defaultDB, releasedDB);
        }
        releasingController.unreleaseDocuments(verticesToBeUnreleased, releasedDB);

    }

    @Override
    void transactionalJsonLdDeletion(String entityName, String key, Integer rootRev) {
        Map instance = repository.getByKey(entityName, key, Map.class, defaultDB);
        if (instance != null) {
            if (releasingController.isRelevantForReleasing(instance)) {
                releasingController.unreleaseVertices(instance, releasedDB);
            }
            if(spatialSearchController.isRelevant(instance)){
                spatialSearchController.remove(instance);
            }
            repository.deleteVertex(entityName, key, defaultDB);
        } else {
            logger.error("DEL: Was not able to find entity {}/{} in repository", entityName, key);
        }
    }

    @Override
    public void clearGraph() {
        repository.clearDatabase(defaultDB.getOrCreateDB());
        if (releasedDB != null) {
            repository.clearDatabase(releasedDB.getOrCreateDB());
        }
    }
}
