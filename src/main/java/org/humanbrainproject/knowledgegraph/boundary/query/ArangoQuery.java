package org.humanbrainproject.knowledgegraph.boundary.query;

import com.github.jsonldjava.utils.JsonUtils;
import com.google.gson.Gson;
import org.humanbrainproject.knowledgegraph.control.arango.ArangoDriver;
import org.humanbrainproject.knowledgegraph.control.arango.query.ArangoSpecificationQuery;
import org.humanbrainproject.knowledgegraph.control.arango.ArangoRepository;
import org.humanbrainproject.knowledgegraph.control.authorization.AuthorizationController;
import org.humanbrainproject.knowledgegraph.control.jsonld.JsonLdStandardization;
import org.humanbrainproject.knowledgegraph.control.specification.SpecificationInterpreter;
import org.humanbrainproject.knowledgegraph.control.template.MustacheTemplating;
import org.humanbrainproject.knowledgegraph.entity.specification.Specification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.configurationprocessor.json.JSONException;
import org.springframework.boot.configurationprocessor.json.JSONObject;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class ArangoQuery {

    public static final String SPECIFICATION_QUERIES = "specification_queries";

    public static final String SPECIFICATION_TEMPLATES = "specification_templates";

    private static final Gson GSON = new Gson();

    @Autowired
    @Qualifier("default")
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

    @Autowired
    AuthorizationController authorization;


    public List<Object> queryPropertyGraphBySpecification(String specification, boolean useContext, String authorizationToken, Integer size, Integer start) throws JSONException, IOException {
        Set<String> readableOrganizations = authorization.getOrganizations(authorizationToken);
        Object originalContext = null;
        if(useContext){
            originalContext = standardization.getContext(specification);
        }
        Specification spec = specInterpreter.readSpecification(new JSONObject( JsonUtils.toString(standardization.fullyQualify(specification))));
        List<Object> objects = specificationQuery.queryForSpecification(spec, readableOrganizations, size, start);
        if(originalContext!=null){
            objects = standardization.applyContext(objects, originalContext);
        }
        return objects;
    }

    public List<Object> queryPropertyGraphByStoredSpecification(String id, boolean useContext, String authorizationToken, Integer size, Integer start) throws IOException, JSONException {
        String payload = arangoUploader.getByKey(SPECIFICATION_QUERIES, id, String.class, arango);
        return queryPropertyGraphBySpecification(payload, useContext, authorizationToken, size, start);
    }

    public void storeSpecificationInDb(String specification, String id, String authorizationToken) throws JSONException {
        JSONObject jsonObject = new JSONObject(specification);
        jsonObject.put("_key", id);
        jsonObject.put("_id", id);
        arangoUploader.insertVertexDocument(jsonObject.toString(), SPECIFICATION_QUERIES, arango);
    }

    public List<Object> queryPropertyGraphByStoredSpecificationAndTemplate(String id, String template, String authorizationToken, Integer size, Integer start) throws IOException, JSONException {
        List<Object> objects = queryPropertyGraphByStoredSpecification(id, true, authorizationToken, size, start);
        return objects.stream().map(o -> GSON.fromJson(templating.applyTemplate(template, o), Map.class)).collect(Collectors.toList());
    }

    public List<Map> getGraph(String rootVertex, Integer step) throws IOException {
        return arangoUploader.inDepthGraph(rootVertex, step, arango);
    }

}
