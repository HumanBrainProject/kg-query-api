package org.humanbrainproject.knowledgegraph.boundary.indexation;

import com.arangodb.ArangoDatabase;
import com.arangodb.entity.CollectionEntity;
import org.humanbrainproject.knowledgegraph.control.arango.ArangoDriver;
import org.humanbrainproject.knowledgegraph.control.arango.query.ArangoSpecificationQuery;
import org.humanbrainproject.knowledgegraph.control.arango.ArangoRepository;
import org.humanbrainproject.knowledgegraph.control.jsonld.JsonLdStandardization;
import org.humanbrainproject.knowledgegraph.control.specification.SpecificationInterpreter;
import org.humanbrainproject.knowledgegraph.entity.jsonld.JsonLdVertex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.configurationprocessor.json.JSONException;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ArangoIndexation extends GraphIndexation {

    @Autowired
    ArangoDriver arango;

    @Autowired
    ArangoRepository repository;

    @Autowired
    SpecificationInterpreter specInterpreter;

    @Autowired
    ArangoSpecificationQuery specificationQuery;

    @Autowired
    JsonLdStandardization standardization;

    public String getById(String entityName, String id){
        return repository.getById(entityName, id, arango);
    }

    @Override
    void transactionalJsonLdInsertion(List<JsonLdVertex> jsonLdVertices) throws JSONException {
        repository.uploadToPropertyGraph(jsonLdVertices, arango);
    }

    @Override
    void transactionalJsonLdUpdate(List<JsonLdVertex> jsonLdVertices) throws JSONException {
        repository.uploadToPropertyGraph(jsonLdVertices, arango);
    }

    @Override
    void transactionalJsonLdDeletion(String entityName, String rootId, Integer rootRev) throws JSONException {
        repository.deleteVertex(entityName, rootId, arango);
    }

    @Override
    public void clearGraph() {
        ArangoDatabase db = arango.getOrCreateDB();
        for (CollectionEntity collectionEntity : db.getCollections()) {
            if(!collectionEntity.getName().startsWith("_")) {
                db.collection(collectionEntity.getName()).drop();
            }
        }
    }
}
