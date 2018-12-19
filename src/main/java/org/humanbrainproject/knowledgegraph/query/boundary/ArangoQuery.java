package org.humanbrainproject.knowledgegraph.query.boundary;

import com.arangodb.ArangoCollection;
import com.github.jsonldjava.core.JsonLdConsts;
import com.github.jsonldjava.utils.JsonUtils;
import org.humanbrainproject.knowledgegraph.commons.authorization.control.AuthorizationController;
import org.humanbrainproject.knowledgegraph.commons.authorization.entity.Credential;
import org.humanbrainproject.knowledgegraph.commons.jsonld.control.JsonLdStandardization;
import org.humanbrainproject.knowledgegraph.commons.jsonld.control.JsonTransformer;
import org.humanbrainproject.knowledgegraph.commons.nexus.control.NexusConfiguration;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.ArangoDatabaseFactory;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.ArangoInternalRepository;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.ArangoRepository;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.query.ArangoSpecificationQuery;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoCollectionReference;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoDocumentReference;
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
import java.util.stream.Collectors;

@Component
public class ArangoQuery {

    public static final ArangoCollectionReference SPECIFICATION_QUERIES = new ArangoCollectionReference("specification_queries");

    @Autowired
    ArangoDatabaseFactory databaseFactory;

    @Autowired
    ArangoRepository arangoRepository;

    @Autowired
    ArangoInternalRepository arangoInternalRepository;


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


    public QueryResult<List<Map>> metaQueryBySpecification(String specification, QueryParameters parameters, NexusSchemaReference schemaReference) throws JSONException, IOException {
        Specification spec = specInterpreter.readSpecification(JsonUtils.toString(standardization.fullyQualify(specification)), schemaReference);
        return specificationQuery.metaSpecification(spec, parameters);
    }

    public Map reflectQueryBySpecification(String specification, NexusSchemaReference schemaReference, QueryParameters parameters, ArangoDocumentReference documentReference, Credential credential) throws JSONException, IOException {
        Specification spec = specInterpreter.readSpecification(JsonUtils.toString(standardization.fullyQualify(specification)), schemaReference);
        Map map = specificationQuery.reflectSpecification(spec, parameters, documentReference, credential);
        map.put("children", regroup((List<Map>)map.get("children")));
        return map;
    }


    private List<Map> regroup(List<Map> children){
        if(children!=null) {
            Map<Object, Map> lookupMap = new HashMap<>();
            for (Map child : children) {
                Object id = child.get(JsonLdConsts.ID);
                if(!lookupMap.containsKey(id)){
                    lookupMap.put(id, child);
                }
                else{
                    if(child.get("children") instanceof List) {
                        Map existing = lookupMap.get(id);
                        if (!existing.containsKey("children")) {
                            existing.put("children", new ArrayList<>());
                        }
                        ((List<Map>) existing.get("children")).addAll((List)child.get("children"));
                        existing.put("children", regroup((List<Map>) existing.get("children")));
                    }
                }
            }
            return new ArrayList<>(lookupMap.values());
        }
        return null;

    }


    public QueryResult<List<Map>> queryPropertyGraphBySpecification(String specification, NexusSchemaReference schemaReference,  QueryParameters parameters, ArangoDocumentReference documentReference, Credential credential) throws JSONException, IOException {
        Map<String, Object> context = null;
        if (parameters.resultTransformation()!=null && parameters.resultTransformation().getVocab() != null) {
            context = new LinkedHashMap<>();
            context.put(JsonLdConsts.VOCAB, parameters.resultTransformation().getVocab());
        }
        Specification spec = specInterpreter.readSpecification(JsonUtils.toString(standardization.fullyQualify(specification)), schemaReference);
        QueryResult<List<Map>> result = specificationQuery.queryForSpecification(spec, parameters, documentReference, credential);
        if (context != null) {
            result.setResults(standardization.applyContext(result.getResults(), context));
        }
        return result;
    }

    public boolean doesQueryExist(StoredQueryReference storedQueryReference){
        ArangoDocumentReference documentReference = new ArangoDocumentReference(SPECIFICATION_QUERIES, storedQueryReference.getName());
        ArangoCollection collection = databaseFactory.getInternalDB().getOrCreateDB().collection(documentReference.getCollection().getName());
        if(collection.exists()){
            return collection.documentExists(documentReference.getKey());
        }
        return false;
    }

    public <T> T getQueryPayload(StoredQueryReference queryReference, Class<T> clazz){
        return arangoRepository.getInternalDocumentByKey(new ArangoDocumentReference(SPECIFICATION_QUERIES, queryReference.getName()), clazz);
    }


    public QueryResult<List<Map>> metaQueryPropertyGraphByStoredSpecification(StoredQueryReference queryReference, QueryParameters parameters) throws IOException, JSONException {
        return metaQueryBySpecification(getQueryPayload(queryReference, String.class), parameters, queryReference.getSchemaReference());
    }

    public Map reflectQueryPropertyGraphByStoredSpecification(StoredQueryReference queryReference, QueryParameters parameters, ArangoDocumentReference documentReference, Credential credential) throws IOException, JSONException {
        return reflectQueryBySpecification(getQueryPayload(queryReference, String.class), queryReference.getSchemaReference(), parameters, documentReference, credential);
    }

    public QueryResult<List<Map>> queryPropertyGraphByStoredSpecification(StoredQueryReference queryReference, QueryParameters parameters, ArangoDocumentReference documentReference, Credential credential) throws IOException, JSONException {
        return queryPropertyGraphBySpecification(getQueryPayload(queryReference, String.class), queryReference.getSchemaReference(), parameters, documentReference, credential);
    }

    public void storeSpecificationInDb(String specification, NexusSchemaReference schemaReference,  String id, Credential credential) throws JSONException {
        StoredQueryReference storedQueryReference = new StoredQueryReference(schemaReference, id);
        JSONObject jsonObject = new JSONObject(specification);
        if(schemaReference!=null){
            JSONObject rootSchema = new JSONObject();
            rootSchema.put(JsonLdConsts.ID, nexusConfiguration.getAbsoluteUrl(schemaReference));
            jsonObject.put(GraphQueryKeys.GRAPH_QUERY_ROOT_SCHEMA.getFieldName(), rootSchema);
        }
        id = storedQueryReference.getName();
        jsonObject.put(ArangoVocabulary.KEY, id);
        jsonObject.put(ArangoVocabulary.ID, id);
        ArangoDocumentReference document = new ArangoDocumentReference(SPECIFICATION_QUERIES, id);
        arangoInternalRepository.insertOrUpdateDocument(document, jsonObject.toString());
    }

    public QueryResult<List<Map>> queryPropertyGraphByStoredSpecificationAndFreemarkerTemplate(StoredQueryReference queryReference, String templatePayload, QueryParameters parameters, Credential credential) throws IOException, JSONException {
        QueryResult<List<Map>> queryResult = queryPropertyGraphByStoredSpecification(queryReference, parameters, null, credential);
        String result = freemarkerTemplating.applyTemplate(templatePayload, queryResult, parameters.context().getLibrary(), databaseFactory.getInternalDB());
        return createResult(queryResult, jsonTransformer.parseToListOfMaps(result), parameters.context().isReturnOriginalJson());
    }

    public Map queryPropertyGraphByStoredSpecificationAndFreemarkerTemplateWithId(StoredQueryReference queryReference, String templatePayload, QueryParameters parameters, NexusInstanceReference instance, Credential credential) throws IOException, JSONException {
        QueryResult<List<Map>> queryResult = queryPropertyGraphByStoredSpecification(queryReference, parameters, ArangoDocumentReference.fromNexusInstance(instance), credential);
        if(instance != null){
            if(queryResult.getResults().size() >= 1){
              String result = freemarkerTemplating.applyTemplate(templatePayload, queryResult, parameters.context().getLibrary(), databaseFactory.getInternalDB());
              return jsonTransformer.parseToMap(result);
            }
        }
        return null;
    }


    public QueryResult<Map> metaQueryPropertyGraphByStoredSpecificationAndFreemarkerTemplate(StoredQueryReference queryReference, Template template, QueryParameters parameters) throws IOException, JSONException {
        QueryResult<List<Map>> queryResult = metaQueryPropertyGraphByStoredSpecification(queryReference, parameters);
        String result = freemarkerTemplating.applyTemplate(template.getTemplateContent(), queryResult, parameters.context().getLibrary(), databaseFactory.getInternalDB());
        Map map = jsonTransformer.parseToMap(result);
        return createResult(queryResult, map, parameters.context().isReturnOriginalJson());
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

    public Set<String> getAllQueryKeys(){
        return arangoInternalRepository.getAll(SPECIFICATION_QUERIES, Map.class).stream().map(q -> (String)q.get(ArangoVocabulary.KEY)).collect(Collectors.toSet());
    }
}
