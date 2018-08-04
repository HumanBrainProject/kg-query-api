package org.humanbrainproject.knowledgegraph.boundary.indexation;

import com.arangodb.ArangoDatabase;
import com.arangodb.entity.CollectionEntity;
import org.humanbrainproject.knowledgegraph.control.arango.ArangoDefaultDatabaseDriver;
import org.humanbrainproject.knowledgegraph.control.arango.ArangoReleasedDatabaseDriver;
import org.humanbrainproject.knowledgegraph.control.arango.query.ArangoSpecificationQuery;
import org.humanbrainproject.knowledgegraph.control.arango.ArangoRepository;
import org.humanbrainproject.knowledgegraph.control.jsonld.JsonLdStandardization;
import org.humanbrainproject.knowledgegraph.control.releasing.ReleasingController;
import org.humanbrainproject.knowledgegraph.control.specification.SpecificationInterpreter;
import org.humanbrainproject.knowledgegraph.entity.jsonld.JsonLdVertex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.configurationprocessor.json.JSONException;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ArangoIndexation extends GraphIndexation {

    @Autowired
    ArangoDefaultDatabaseDriver defaultDB;

    @Autowired
    ArangoReleasedDatabaseDriver releasedDB;

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
        return repository.getById(entityName, id, defaultDB);
    }

    @Override
    void transactionalJsonLdInsertion(List<JsonLdVertex> jsonLdVertices) throws JSONException {
        repository.uploadToPropertyGraph(jsonLdVertices, defaultDB);
        if(releasingController.isRelevantForReleasing(jsonLdVertices)){
            //Upload to released database
            List<JsonLdVertex> verticesToBeReleased = releasingController.getVerticesToBeReleased(jsonLdVertices);
            repository.uploadToPropertyGraph(verticesToBeReleased, releasedDB);
        }
    }

    @Override
    void transactionalJsonLdUpdate(List<JsonLdVertex> jsonLdVertices) throws JSONException {
        repository.uploadToPropertyGraph(jsonLdVertices, defaultDB);
        if(releasingController.isRelevantForReleasing(jsonLdVertices)){
            //Upload to released database
            List<JsonLdVertex> verticesToBeReleased = releasingController.getVerticesToBeReleased(jsonLdVertices);
            repository.uploadToPropertyGraph(verticesToBeReleased, releasedDB);
        }
    }

    @Override
    void transactionalJsonLdDeletion(String entityName, String rootId, Integer rootRev) {
        repository.deleteVertex(entityName, rootId, defaultDB);
        repository.deleteVertex(entityName, rootId, releasedDB);
    }

    @Override
    public void clearGraph() {
        ArangoDatabase db = defaultDB.getOrCreateDB();
        for (CollectionEntity collectionEntity : db.getCollections()) {
            if(!collectionEntity.getName().startsWith("_")) {
                db.collection(collectionEntity.getName()).drop();
            }
        }
        db = releasedDB.getOrCreateDB();
        for (CollectionEntity collectionEntity : db.getCollections()) {
            if(!collectionEntity.getName().startsWith("_")) {
                db.collection(collectionEntity.getName()).drop();
            }
        }
    }
}
