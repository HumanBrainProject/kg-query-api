package org.humanbrainproject.knowledgegraph.boundary.query;

import com.github.jsonldjava.utils.JsonUtils;
import org.humanbrainproject.knowledgegraph.control.arango.ArangoDriver;
import org.humanbrainproject.knowledgegraph.control.arango.query.ArangoSpecificationQuery;
import org.humanbrainproject.knowledgegraph.control.arango.ArangoRepository;
import org.humanbrainproject.knowledgegraph.control.jsonld.JsonLdStandardization;
import org.humanbrainproject.knowledgegraph.control.specification.SpecificationInterpreter;
import org.humanbrainproject.knowledgegraph.entity.specification.Specification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.configurationprocessor.json.JSONException;
import org.springframework.boot.configurationprocessor.json.JSONObject;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

@Component
public class ArangoQuery {

    public static final String SPECIFICATION_QUERIES = "specification_queries";

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


    public List<Object> queryPropertyGraphBySpecification(String specification) throws JSONException, IOException {
        Specification spec = specInterpreter.readSpecification(new JSONObject( JsonUtils.toString(standardization.fullyQualify(specification))));
        return specificationQuery.queryForSpecification(spec);
    }

    public List<Object> queryPropertyGraphByStoredSpecification(String id) throws IOException, JSONException {
        String payload = arangoUploader.getById(SPECIFICATION_QUERIES, id, arango);
        return queryPropertyGraphBySpecification(payload);
    }

    public void storeSpecificationInDb(String specification, String id) throws JSONException {
        JSONObject jsonObject = new JSONObject(specification);
        jsonObject.put("_key", id);
        jsonObject.put("_id", id);
        arangoUploader.insertVertexDocument(jsonObject.toString(), SPECIFICATION_QUERIES, arango);
    }
}
