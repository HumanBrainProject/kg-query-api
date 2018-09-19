package org.humanbrainproject.knowledgegraph.boundary.query;

import com.github.jsonldjava.core.JsonLdConsts;
import com.github.jsonldjava.utils.JsonUtils;
import com.google.gson.Gson;
import org.humanbrainproject.knowledgegraph.control.arango.ArangoDriver;
import org.humanbrainproject.knowledgegraph.control.arango.ArangoRepository;
import org.humanbrainproject.knowledgegraph.control.arango.query.ArangoSpecificationQuery;
import org.humanbrainproject.knowledgegraph.control.authorization.AuthorizationController;
import org.humanbrainproject.knowledgegraph.control.jsonld.JsonLdStandardization;
import org.humanbrainproject.knowledgegraph.control.specification.SpecificationInterpreter;
import org.humanbrainproject.knowledgegraph.control.template.FreemarkerTemplating;
import org.humanbrainproject.knowledgegraph.control.template.MustacheTemplating;
import org.humanbrainproject.knowledgegraph.entity.query.QueryParameters;
import org.humanbrainproject.knowledgegraph.entity.query.QueryResult;
import org.humanbrainproject.knowledgegraph.entity.specification.Specification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.configurationprocessor.json.JSONException;
import org.springframework.boot.configurationprocessor.json.JSONObject;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;

@Component
public class ArangoQuery {

    public static final String SPECIFICATION_QUERIES = "specification_queries";

    public static final String SPECIFICATION_TEMPLATES = "specification_templates";

    private static final Gson GSON = new Gson();

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


    private Set<String> getReadableOrganizations(String authorizationToken, String organizations){
        String[] whitelisted = null;
        if(organizations!=null){
            whitelisted = organizations.split(",");
        }
//        Set<String> readableOrganizations = authorization.getOrganizations(authorizationToken);
        Set<String> readableOrganizations = new LinkedHashSet<>();
        readableOrganizations.add("minds");
        readableOrganizations.add("brainviewer");
        readableOrganizations.add("cscs");
        readableOrganizations.add("datacite");
        readableOrganizations.add("licenses");
        readableOrganizations.add("minds2");
        readableOrganizations.add("neuroglancer");
        readableOrganizations.add("kgeditor");
        if(whitelisted!=null){
            readableOrganizations.retainAll(Arrays.asList(whitelisted));
        }
        return readableOrganizations;
    }

    public QueryResult<List<Map>> metaQueryBySpecification(String specification, QueryParameters parameters) throws JSONException, IOException {
        Specification spec = specInterpreter.readSpecification(JsonUtils.toString(standardization.fullyQualify(specification)));
        return specificationQuery.metaSpecification(spec, parameters);
    }

    public QueryResult<List<Map>> queryPropertyGraphBySpecification(String specification, QueryParameters parameters, String instanceId) throws JSONException, IOException {
        Set<String> readableOrganizations = getReadableOrganizations(parameters.authorizationToken, parameters.organizations);
        Map<String, Object> context = null;
        if (parameters.vocab!=null) {
            context = new LinkedHashMap<>();
            context.put(JsonLdConsts.VOCAB, parameters.vocab);
        }
        Specification spec = specInterpreter.readSpecification(JsonUtils.toString(standardization.fullyQualify(specification)));
        QueryResult<List<Map>> result = specificationQuery.queryForSpecification(spec, readableOrganizations, parameters,instanceId);
        if (context != null) {
            result.setResults(standardization.applyContext(result.getResults(), context));
        }
        return result;
    }

    public QueryResult<List<Map>> metaQueryPropertyGraphByStoredSpecification(String id, QueryParameters parameters) throws IOException, JSONException {
        String payload = arangoUploader.getByKey(SPECIFICATION_QUERIES, id, String.class, arangoInternal);
        return metaQueryBySpecification(payload, parameters);
    }

    public QueryResult<List<Map>> queryPropertyGraphByStoredSpecification(String id, QueryParameters parameters, String instanceId) throws IOException, JSONException {
        String payload = arangoUploader.getByKey(SPECIFICATION_QUERIES, id, String.class, arangoInternal);
        return queryPropertyGraphBySpecification(payload, parameters, instanceId);
    }

    public void storeSpecificationInDb(String specification, String id) throws JSONException {
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

    public QueryResult<String> queryPropertyGraphByStoredSpecificationAndFreemarkerTemplate(String id, String template, QueryParameters parameters) throws IOException, JSONException {
        QueryResult<List<Map>> queryResult = queryPropertyGraphByStoredSpecification(id, parameters, null);
        return createResult(queryResult, freemarkerTemplating.applyTemplate(template, queryResult, parameters.library, arangoInternal), parameters.withOriginalJson);
    }

    public Map queryPropertyGraphByStoredSpecificationAndFreemarkerTemplateWithId(String id, String template, QueryParameters parameters, String instanceId) throws IOException, JSONException {
        QueryResult<List<Map>> queryResult = queryPropertyGraphByStoredSpecification(id, parameters, instanceId);
        if(instanceId != null){
            if(queryResult.getResults().size() >= 1){
              return queryResult.getResults().get(0);
            }
        }
        return null;
    }



    public QueryResult<String> metaQueryPropertyGraphByStoredSpecificationAndFreemarkerTemplate(String id, String template, QueryParameters parameters) throws IOException, JSONException {
        QueryResult<List<Map>> queryResult = metaQueryPropertyGraphByStoredSpecification(id, parameters);
        return createResult(queryResult, freemarkerTemplating.applyTemplate(template, queryResult, "meta", arangoInternal), parameters.withOriginalJson);
    }

    private <T> QueryResult<T> createResult(QueryResult<List<Map>> queryResult, T result, boolean addOriginalSource){
        QueryResult<T> r = new QueryResult<>();
        r.setResults(result);
        r.setApiName(queryResult.getApiName());
        r.setTotal(queryResult.getTotal());
        r.setSize(queryResult.getSize());
        r.setStart(queryResult.getSize());
        if(addOriginalSource) {
            r.setOriginalJson(queryResult.getResults());
        }
        return r;
    }

    public QueryResult applyFreemarkerOnMetaQueryBasedOnTemplate(String metaTemplate, String targetTemplate, String queryId, QueryParameters parameters) throws IOException, JSONException {
        QueryResult<List<Map>> queryResult = metaQueryPropertyGraphByStoredSpecification(queryId, parameters);
        String meta = freemarkerTemplating.applyTemplate(metaTemplate, queryResult, "meta", arangoInternal);
        QueryResult metaQueryResult = createResult(queryResult, GSON.fromJson(meta, List.class), parameters.withOriginalJson);
        String finalPayload = freemarkerTemplating.applyTemplate(targetTemplate, metaQueryResult, null, arangoInternal);
        return createResult(queryResult, finalPayload, parameters.withOriginalJson);
    }
}
