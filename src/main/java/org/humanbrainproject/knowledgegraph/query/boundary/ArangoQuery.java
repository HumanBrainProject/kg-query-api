package org.humanbrainproject.knowledgegraph.query.boundary;

import com.arangodb.entity.CollectionType;
import com.github.jsonldjava.core.JsonLdConsts;
import com.github.jsonldjava.utils.JsonUtils;
import com.google.gson.Gson;
import org.humanbrainproject.knowledgegraph.commons.authorization.control.AuthorizationController;
import org.humanbrainproject.knowledgegraph.commons.authorization.entity.OidcAccessToken;
import org.humanbrainproject.knowledgegraph.commons.jsonld.control.JsonLdStandardization;
import org.humanbrainproject.knowledgegraph.commons.jsonld.control.JsonTransformer;
import org.humanbrainproject.knowledgegraph.commons.nexus.control.NexusConfiguration;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.ArangoDatabaseFactory;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.ArangoRepository;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.query.ArangoSpecificationQuery;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoCollectionReference;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoDocumentReference;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoNamingHelper;
import org.humanbrainproject.knowledgegraph.commons.vocabulary.ArangoVocabulary;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusInstanceReference;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusSchemaReference;
import org.humanbrainproject.knowledgegraph.query.control.FreemarkerTemplating;
import org.humanbrainproject.knowledgegraph.query.control.SpecificationInterpreter;
import org.humanbrainproject.knowledgegraph.query.entity.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.configurationprocessor.json.JSONException;
import org.springframework.boot.configurationprocessor.json.JSONObject;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;

@Component
public class ArangoQuery {

    public static final ArangoCollectionReference SPECIFICATION_QUERIES = new ArangoCollectionReference("specification_queries");

    public static final ArangoCollectionReference SPECIFICATION_TEMPLATES = new ArangoCollectionReference("specification_templates");

    private static final Gson GSON = new Gson();
    public static final StoredLibraryReference META_LIBRARY = new StoredLibraryReference("meta");

    @Autowired
    ArangoDatabaseFactory databaseFactory;

    @Autowired
    ArangoRepository arangoRepository;

    @Autowired
    SpecificationInterpreter specInterpreter;

    @Autowired
    ArangoSpecificationQuery specificationQuery;

    @Autowired
    JsonLdStandardization standardization;

    @Autowired
    FreemarkerTemplating freemarkerTemplating;

    @Autowired
    AuthorizationController authorization;

    @Autowired
    NexusConfiguration nexusConfiguration;

    @Autowired
    JsonTransformer jsonTransformer;

    private Set<String> getReadableOrganizations(OidcAccessToken authorizationToken, List<String> whitelistedOrganizations){
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
        readableOrganizations.add("hbpkg");
        if(whitelistedOrganizations!=null){
            readableOrganizations.retainAll(whitelistedOrganizations);
        }
        return readableOrganizations;
    }

    public QueryResult<List<Map>> metaQueryBySpecification(String specification, QueryParameters parameters, NexusSchemaReference schemaReference) throws JSONException, IOException {
        Specification spec = specInterpreter.readSpecification(JsonUtils.toString(standardization.fullyQualify(specification)), schemaReference);
        return specificationQuery.metaSpecification(spec, parameters);
    }

    public QueryResult<List<Map>> queryPropertyGraphBySpecification(String specification, NexusSchemaReference schemaReference,  QueryParameters parameters, ArangoDocumentReference documentReference) throws JSONException, IOException {
        Set<String> readableOrganizations = getReadableOrganizations(parameters.authorization(), parameters.filter().getRestrictToOrganizations());
        Map<String, Object> context = null;
        if (parameters.resultTransformation()!=null && parameters.resultTransformation().getVocab() != null) {
            context = new LinkedHashMap<>();
            context.put(JsonLdConsts.VOCAB, parameters.resultTransformation().getVocab());
        }
        Specification spec = specInterpreter.readSpecification(JsonUtils.toString(standardization.fullyQualify(specification)), schemaReference);

        QueryResult<List<Map>> result = specificationQuery.queryForSpecification(spec, readableOrganizations, parameters, documentReference);
        if (context != null) {
            result.setResults(standardization.applyContext(result.getResults(), context));
        }
        return result;
    }

    public QueryResult<List<Map>> metaQueryPropertyGraphByStoredSpecification(StoredQueryReference storedQueryReference, QueryParameters parameters) throws IOException, JSONException {
        String payload = arangoRepository.getByKey(new ArangoDocumentReference(SPECIFICATION_QUERIES, storedQueryReference.getName()), String.class, databaseFactory.getInternalDB());
        return metaQueryBySpecification(payload, parameters, null);
    }

    public QueryResult<List<Map>> queryPropertyGraphByStoredSpecification(StoredQueryReference queryReference, QueryParameters parameters, ArangoDocumentReference documentReference) throws IOException, JSONException {
        String payload = arangoRepository.getByKey(new ArangoDocumentReference(SPECIFICATION_QUERIES,  ArangoNamingHelper.createCompatibleId(queryReference.getName())), String.class, databaseFactory.getInternalDB());
        return queryPropertyGraphBySpecification(payload, null, parameters, documentReference);
    }

    public void storeSpecificationInDb(String specification, NexusSchemaReference schemaReference,  String id) throws JSONException {
        StoredQueryReference storedQueryReference = new StoredQueryReference(schemaReference, id);
        JSONObject jsonObject = new JSONObject(specification);
        if(schemaReference!=null){
            JSONObject rootSchema = new JSONObject();
            rootSchema.put(JsonLdConsts.ID, nexusConfiguration.getAbsoluteUrl(schemaReference));
            jsonObject.put(GraphQueryKeys.GRAPH_QUERY_ROOT_SCHEMA.getFieldName(), rootSchema);
        }
        id = ArangoNamingHelper.createCompatibleId(storedQueryReference.getName());
        jsonObject.put(ArangoVocabulary.KEY, id);
        jsonObject.put(ArangoVocabulary.ID, id);
        ArangoDocumentReference document = new ArangoDocumentReference(SPECIFICATION_QUERIES, id);
        Map spec = arangoRepository.getByKey(document, Map.class, databaseFactory.getInternalDB());
        if (spec != null) {
            arangoRepository.replaceDocument(document, jsonObject.toString(), databaseFactory.getInternalDB());
        }
        else {
            arangoRepository.insertDocument(document, jsonObject.toString(), CollectionType.DOCUMENT, databaseFactory.getInternalDB().getOrCreateDB());
        }
    }

    public QueryResult<List<Map>> queryPropertyGraphByStoredSpecificationAndFreemarkerTemplate(StoredQueryReference queryReference, String templatePayload, StoredLibraryReference library, QueryParameters parameters) throws IOException, JSONException {
        QueryResult<List<Map>> queryResult = queryPropertyGraphByStoredSpecification(queryReference, parameters, null);
        String result = freemarkerTemplating.applyTemplate(templatePayload, queryResult, library, databaseFactory.getInternalDB());
        return createResult(queryResult, jsonTransformer.parseToListOfMaps(result), parameters.context().isReturnOriginalJson());
    }

    public Map queryPropertyGraphByStoredSpecificationAndFreemarkerTemplateWithId(StoredQueryReference queryReference, String templatePayload, QueryParameters parameters, NexusInstanceReference instance) throws IOException, JSONException {
        QueryResult<List<Map>> queryResult = queryPropertyGraphByStoredSpecification(queryReference, parameters, ArangoDocumentReference.fromNexusInstance(instance));
        if(instance != null){
            if(queryResult.getResults().size() >= 1){
              return queryResult.getResults().get(0);
            }
        }
        return null;
    }



    public QueryResult<Map> metaQueryPropertyGraphByStoredSpecificationAndFreemarkerTemplate(StoredQueryReference queryReference, String template, QueryParameters parameters) throws IOException, JSONException {
        QueryResult<List<Map>> queryResult = metaQueryPropertyGraphByStoredSpecification(queryReference, parameters);
        String result = freemarkerTemplating.applyTemplate(template, queryResult, META_LIBRARY, databaseFactory.getInternalDB());
        return createResult(queryResult, jsonTransformer.parseToMap(result), parameters.context().isReturnOriginalJson());
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

    public QueryResult applyFreemarkerOnMetaQueryBasedOnTemplate(String metaTemplate, String targetTemplate, StoredQueryReference queryReference, QueryParameters parameters) throws IOException, JSONException {
        QueryResult<List<Map>> queryResult = metaQueryPropertyGraphByStoredSpecification(queryReference, parameters);
        String meta = freemarkerTemplating.applyTemplate(metaTemplate, queryResult, META_LIBRARY, databaseFactory.getInternalDB());
        QueryResult metaQueryResult = createResult(queryResult, GSON.fromJson(meta, List.class), parameters.context().isReturnOriginalJson());
        String finalPayload = freemarkerTemplating.applyTemplate(targetTemplate, metaQueryResult, null, databaseFactory.getInternalDB());
        return createResult(queryResult, finalPayload, parameters.context().isReturnOriginalJson());
    }
}
