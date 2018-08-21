package org.humanbrainproject.knowledgegraph.boundary.query;

import com.github.jsonldjava.utils.JsonUtils;
import com.google.gson.Gson;
import org.humanbrainproject.knowledgegraph.control.arango.ArangoDriver;
import org.humanbrainproject.knowledgegraph.control.arango.query.ArangoSpecificationQuery;
import org.humanbrainproject.knowledgegraph.control.arango.ArangoRepository;
import org.humanbrainproject.knowledgegraph.control.authorization.AuthorizationController;
import org.humanbrainproject.knowledgegraph.control.jsonld.JsonLdStandardization;
import org.humanbrainproject.knowledgegraph.control.specification.SpecificationInterpreter;
import org.humanbrainproject.knowledgegraph.control.template.FreemarkerTemplating;
import org.humanbrainproject.knowledgegraph.control.template.MustacheTemplating;
import org.humanbrainproject.knowledgegraph.entity.Template;
import org.humanbrainproject.knowledgegraph.entity.query.QueryResult;
import org.humanbrainproject.knowledgegraph.entity.specification.Specification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.configurationprocessor.json.JSONException;
import org.springframework.boot.configurationprocessor.json.JSONObject;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

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
    @Qualifier("internal")
    ArangoDriver arangoInternal;

    @Autowired
    ArangoRepository arangoUploader;

    @Autowired
    SpecificationInterpreter specInterpreter;

    @Autowired
    ArangoSpecificationQuery specificationQuery;

    @Autowired
    JsonLdStandardization standardization;

    @Autowired
    MustacheTemplating mustacheTemplating;

    @Autowired
    FreemarkerTemplating freemarkerTemplating;

    @Autowired
    AuthorizationController authorization;


    private Set<String> getReadableOrganizations(String authorizationToken){
        //Set<String> readableOrganizations = authorization.getOrganizations(authorizationToken);
        Set<String> readableOrganizations = new LinkedHashSet<>();
        readableOrganizations.add("minds");
        readableOrganizations.add("brainviewer");
        readableOrganizations.add("cscs");
        readableOrganizations.add("datacite");
        readableOrganizations.add("licenses");
        readableOrganizations.add("minds2");
        readableOrganizations.add("neuroglancer");
        return readableOrganizations;
    }

    public QueryResult<List<Map>> reflectQueryBySpecification(String specificationId, String specification, String authorizationToken, Integer size, Integer start) throws JSONException, IOException {
        Set<String> readableOrganizations = getReadableOrganizations(authorizationToken);
        Specification spec = specInterpreter.readSpecification(JsonUtils.toString(standardization.fullyQualify(specification)));
        spec.setSpecificationId(specificationId);
        QueryResult<List<Map>> result = specificationQuery.queryForSpecification(spec, readableOrganizations, size, start);
        return result;
    }

    public QueryResult<List<Map>> metaQueryBySpecification(String specification, String authorizationToken) throws JSONException, IOException {
        Specification spec = specInterpreter.readSpecification(JsonUtils.toString(standardization.fullyQualify(specification)));
        return specificationQuery.metaSpecification(spec);
    }

    public QueryResult<List<Map>> queryPropertyGraphBySpecification(String specification, boolean useContext, String authorizationToken, Integer size, Integer start) throws JSONException, IOException {
        Set<String> readableOrganizations = getReadableOrganizations(authorizationToken);
        Object originalContext = null;
        if (useContext) {
            originalContext = standardization.getContext(specification);
        }
        Specification spec = specInterpreter.readSpecification(JsonUtils.toString(standardization.fullyQualify(specification)));
        QueryResult<List<Map>> result = specificationQuery.queryForSpecification(spec, readableOrganizations, size, start);
        if (originalContext != null) {
            result.setResults(standardization.applyContext(result.getResults(), originalContext));
        }
        return result;
    }

    public QueryResult<List<Map>> metaQueryPropertyGraphByStoredSpecification(String id, String authorizationToken) throws IOException, JSONException {
        String payload = arangoUploader.getByKey(SPECIFICATION_QUERIES, id, String.class, arangoInternal);
        return metaQueryBySpecification(payload, authorizationToken);
    }

    public QueryResult<List<Map>> queryPropertyGraphByStoredSpecification(String id, boolean useContext, String authorizationToken, Integer size, Integer start) throws IOException, JSONException {
        String payload = arangoUploader.getByKey(SPECIFICATION_QUERIES, id, String.class, arangoInternal);
        return queryPropertyGraphBySpecification(payload, useContext, authorizationToken, size, start);
    }

    public void storeSpecificationInDb(String specification, String id, String authorizationToken) throws JSONException {
        JSONObject jsonObject = new JSONObject(specification);
        jsonObject.put("_key", id);
        jsonObject.put("_id", id);
        Map spec = arangoUploader.getByKey(SPECIFICATION_QUERIES, id, Map.class, arangoInternal);
        if (spec != null) {
            arangoUploader.replaceDocument(SPECIFICATION_QUERIES, id, jsonObject.toString(), arangoInternal);
        } else {
            arangoUploader.insertVertexDocument(jsonObject.toString(), SPECIFICATION_QUERIES, arangoInternal);
        }
    }

    public QueryResult queryPropertyGraphByStoredSpecificationAndMustacheTemplate(String id, String template, String authorizationToken, Integer size, Integer start) throws IOException, JSONException {
        QueryResult<List<Map>> queryResult = queryPropertyGraphByStoredSpecification(id, true, authorizationToken, size, start);
        List<Map> transformedInstances = queryResult.getResults().stream().map(o -> GSON.fromJson(mustacheTemplating.applyTemplate(template, o), Map.class)).collect(Collectors.toList());
        queryResult.setResults(transformedInstances);
        return queryResult;
    }

    public String queryPropertyGraphByStoredSpecificationAndFreemarkerTemplate(String id, String template, String authorizationToken, Integer size, Integer start, String library) throws IOException, JSONException {
        QueryResult<List<Map>> queryResult = queryPropertyGraphByStoredSpecification(id, false, authorizationToken, size, start);
        return freemarkerTemplating.applyTemplate(template, queryResult, library, arangoInternal);
    }


    public String metaQueryPropertyGraphByStoredSpecificationAndFreemarkerTemplate(String id, String template, String authorizationToken) throws IOException, JSONException {
        QueryResult<List<Map>> queryResult = metaQueryPropertyGraphByStoredSpecification(id,authorizationToken);
        return freemarkerTemplating.applyTemplate(template, queryResult, "meta", arangoInternal);
    }


    public String applyFreemarkerTemplateToJSON(String json, String template, String authorizationToken) throws IOException, JSONException {
        QueryResult<List<Map>> queryResult = new QueryResult<>();
        queryResult.setResults(GSON.fromJson(json, List.class));
        return freemarkerTemplating.applyTemplate(template, queryResult, null, arangoInternal);
    }


    public String applyFreemarkerOnMetaQueryBasedOnTemplate(String metaTemplate, String targetTemplate, String queryId, String authorization) throws IOException, JSONException {
        QueryResult<List<Map>> queryResult = metaQueryPropertyGraphByStoredSpecification(queryId,authorization);
        String meta = freemarkerTemplating.applyTemplate(metaTemplate, queryResult, "meta", arangoInternal);
        QueryResult<List<Map>> metaQueryResult = new QueryResult<>();
        metaQueryResult.setApiName(queryResult.getApiName());
        metaQueryResult.setResults(GSON.fromJson(meta, List.class));
        return freemarkerTemplating.applyTemplate(targetTemplate, metaQueryResult, null, arangoInternal);
    }
}
