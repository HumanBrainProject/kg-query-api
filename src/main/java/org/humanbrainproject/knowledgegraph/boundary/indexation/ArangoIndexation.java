package org.humanbrainproject.knowledgegraph.boundary.indexation;

import com.arangodb.ArangoDatabase;
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
    ArangoRepository arangoUploader;

    @Autowired
    SpecificationInterpreter specInterpreter;

    @Autowired
    ArangoSpecificationQuery specificationQuery;

    @Autowired
    JsonLdStandardization standardization;

    @Override
    void transactionalJsonLdUpload(List<JsonLdVertex> vertices) throws JSONException {
        arangoUploader.uploadToPropertyGraph(vertices, arango);
    }

    @Override
    public void clearGraph() {
        ArangoDatabase db = arango.getOrCreateDB();
        db.drop();
        db.exists();
    }
}
