package org.humanbrainproject.knowledgegraph.boundary.query;

import com.github.jsonldjava.core.JsonLdConsts;
import com.github.jsonldjava.core.JsonLdOptions;
import com.github.jsonldjava.core.JsonLdProcessor;
import com.github.jsonldjava.core.JsonLdUtils;
import com.github.jsonldjava.utils.JsonUtils;
import com.google.gson.Gson;
import org.humanbrainproject.knowledgegraph.control.arango.ArangoDriver;
import org.humanbrainproject.knowledgegraph.control.arango.query.ArangoSpecificationQuery;
import org.humanbrainproject.knowledgegraph.control.arango.ArangoRepository;
import org.humanbrainproject.knowledgegraph.control.jsonld.JsonLdStandardization;
import org.humanbrainproject.knowledgegraph.control.specification.SpecificationInterpreter;
import org.humanbrainproject.knowledgegraph.control.template.MustacheTemplating;
import org.humanbrainproject.knowledgegraph.entity.specification.Specification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.configurationprocessor.json.JSONException;
import org.springframework.boot.configurationprocessor.json.JSONObject;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;

@Component
public class ArangoQuery {

    public static final String SPECIFICATION_QUERIES = "specification_queries";

    public static final String SPECIFICATION_TEMPLATES = "specification_templates";

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

    @Autowired
    MustacheTemplating templating;


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


    public List<Object> queryPropertyGraphByStoredSpecificationAndTemplate(String id, String template) throws IOException, JSONException {
        String payload = arangoUploader.getById(SPECIFICATION_QUERIES, id, arango);
        Object originalContext = Collections.emptyMap();
        Gson gson = new Gson();
        if(payload!=null){
            JSONObject jsonObject = new JSONObject(payload);
            if(jsonObject.has(JsonLdConsts.CONTEXT)){
                originalContext = gson.fromJson(jsonObject.getJSONObject(JsonLdConsts.CONTEXT).toString(), Map.class);
            }
        }
        List<Object> objects = queryPropertyGraphByStoredSpecification(id);
        List<Object> result = new ArrayList<>();
        for (Object object : objects) {
            Map<String, Object> compact = JsonLdProcessor.compact(object, originalContext, new JsonLdOptions());
            String jsonString = templating.applyTemplate(template, compact);
            System.out.println(jsonString);
            result.add(gson.fromJson(jsonString, Map.class));
        }
        return result;
    }




}
