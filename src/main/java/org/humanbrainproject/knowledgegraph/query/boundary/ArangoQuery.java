package org.humanbrainproject.knowledgegraph.query.boundary;

import com.github.jsonldjava.core.JsonLdConsts;
import com.github.jsonldjava.utils.JsonUtils;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.tinkerpop.gremlin.structure.T;
import org.humanbrainproject.knowledgegraph.annotations.ToBeTested;
import org.humanbrainproject.knowledgegraph.commons.jsonld.control.JsonLdStandardization;
import org.humanbrainproject.knowledgegraph.commons.jsonld.control.JsonTransformer;
import org.humanbrainproject.knowledgegraph.commons.nexus.control.NexusConfiguration;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.ArangoDatabaseFactory;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.ArangoInternalRepository;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.ArangoRepository;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.ArangoToNexusLookupMap;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.builders.TreeScope;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoCollectionReference;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoDocumentReference;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoNamingHelper;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.exceptions.StoredQueryNotFoundException;
import org.humanbrainproject.knowledgegraph.commons.vocabulary.ArangoVocabulary;
import org.humanbrainproject.knowledgegraph.commons.vocabulary.HBPVocabulary;
import org.humanbrainproject.knowledgegraph.commons.vocabulary.SchemaOrgVocabulary;
import org.humanbrainproject.knowledgegraph.context.QueryContext;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusInstanceReference;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusSchemaReference;
import org.humanbrainproject.knowledgegraph.query.control.FreemarkerTemplating;
import org.humanbrainproject.knowledgegraph.query.control.SpatialSearch;
import org.humanbrainproject.knowledgegraph.query.control.SpecificationController;
import org.humanbrainproject.knowledgegraph.query.control.SpecificationInterpreter;
import org.humanbrainproject.knowledgegraph.query.entity.*;
import org.humanbrainproject.knowledgegraph.query.entity.fieldFilter.ParameterDescription;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.configurationprocessor.json.JSONException;
import org.springframework.boot.configurationprocessor.json.JSONObject;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Component
@ToBeTested(integrationTestRequired = true, systemTestRequired = true)
public class ArangoQuery {

    public static final ArangoCollectionReference SPECIFICATION_QUERIES = new ArangoCollectionReference("specification_queries");

    @Autowired
    QueryContext queryContext;

    @Autowired
    ArangoDatabaseFactory databaseFactory;

    @Autowired
    ArangoRepository arangoRepository;

    @Autowired
    ArangoInternalRepository arangoInternalRepository;

    @Autowired
    SpecificationInterpreter specInterpreter;

    @Autowired
    SpecificationController specificationQuery;

    @Autowired
    JsonLdStandardization standardization;

    @Autowired
    FreemarkerTemplating freemarkerTemplating;

    @Autowired
    NexusConfiguration nexusConfiguration;

    @Autowired
    JsonTransformer jsonTransformer;

    @Autowired
    SpatialSearch spatialSearch;

    @Autowired
    Templating templating;

    @Autowired
    ArangoToNexusLookupMap lookupMap;


    private String getAbsoluteUrlOfRootSchema(Query query){
        if(query.getSchemaReference()!=null) {
            return nexusConfiguration.getAbsoluteUrl(query.getSchemaReference());
        }
        return null;
    }

    public List<ParameterDescription> listQueryParameters(StoredQuery storedQuery) throws IOException, JSONException {
        return listQueryParameters(resolveStoredQuery(storedQuery));
    }


    public List<ParameterDescription> listQueryParameters(Query query) throws IOException, JSONException {
        Specification spec = specInterpreter.readSpecification(JsonUtils.toString(standardization.fullyQualify(query.getSpecification())), getAbsoluteUrlOfRootSchema(query), null);
        return spec.getAllFilterParameters();
    }

    public QueryResult<List<Map>> metaQueryBySpecification(Query query) throws JSONException, IOException {
        Specification spec = specInterpreter.readSpecification(JsonUtils.toString(standardization.fullyQualify(query.getSpecification())), getAbsoluteUrlOfRootSchema(query), null);
        return specificationQuery.metaSpecification(spec);
    }


    public Map queryReleaseTree(Query query, NexusInstanceReference instanceReference, TreeScope scope) throws JSONException, IOException {
        Map map;
        if(query == null ||  query.getSpecification() == null){
            map = specificationQuery.defaultReleaseTree(instanceReference);
        }
        else {
            Specification spec = specInterpreter.readSpecification(JsonUtils.toString(standardization.fullyQualify(query.getSpecification())), getAbsoluteUrlOfRootSchema(query), null);
            map = specificationQuery.releaseTreeBySpecification(spec, query, instanceReference, scope);
        }
        map.put("children", regroup((List<Map>) map.get("children")));
        return map;
    }


    private List<Map> regroup(List<Map> children) {
        if (children != null) {
            Map<Object, Map> lookupMap = new HashMap<>();
            for (Map child : children) {
                Object id = child.get(JsonLdConsts.ID);
                if (!lookupMap.containsKey(id)) {
                    lookupMap.put(id, child);
                } else {
                    if (child.get("children") instanceof List) {
                        Map existing = lookupMap.get(id);
                        if (!existing.containsKey("children")) {
                            existing.put("children", new ArrayList<>());
                        }
                        ((List<Map>) existing.get("children")).addAll((List) child.get("children"));
                        existing.put("children", regroup((List<Map>) existing.get("children")));
                    }
                }
            }
            return new ArrayList<>(lookupMap.values());
        }
        return null;
    }


    public QueryResult<List<Map>> queryPropertyGraphBySpecification(Query query, String queryId) throws JSONException, IOException, SolrServerException {
        Map<String, Object> context = null;
        if (query.getVocabulary() != null) {
            context = new LinkedHashMap<>();
            context.put(JsonLdConsts.VOCAB, query.getVocabulary());
        }
        Specification spec = specInterpreter.readSpecification(JsonUtils.toString(standardization.fullyQualify(query.getSpecification())),  getAbsoluteUrlOfRootSchema(query), query.getParameters());
        QueryResult<List<Map>> result = specificationQuery.queryForSpecification(spec, query.getPagination(), query.getFilter(), queryId);
        if (context != null) {
            result.setResults(standardization.applyContext(result.getResults(), context));
        }
        return result;
    }

    public List<Map> getStoredQueries(){
        List<Map> internalDocuments = arangoInternalRepository.getInternalDocuments(SPECIFICATION_QUERIES);
        return internalDocuments;
    }

    public List<Map> getStoredQueriesBySchema(NexusSchemaReference schemaReference){
        List<Map> internalDocuments = arangoInternalRepository.getInternalDocuments(SPECIFICATION_QUERIES);
        List<Map> result = new ArrayList<>();
        for (Map internalDocument : internalDocuments) {
            if(internalDocument.containsKey(GraphQueryKeys.GRAPH_QUERY_ROOT_SCHEMA.getFieldName())){
                String rootSchema = (String)((Map) internalDocument.get(GraphQueryKeys.GRAPH_QUERY_ROOT_SCHEMA.getFieldName())).get(JsonLdConsts.ID);
                NexusSchemaReference fromUrl = NexusSchemaReference.createFromUrl(rootSchema);
                if(schemaReference.getRelativeUrl().getUrl().equals(fromUrl.getRelativeUrl().getUrl())){
                    JsonDocument doc = new JsonDocument(internalDocument);
                    if(doc.containsKey(SchemaOrgVocabulary.IDENTIFIER)){
                        JsonDocument r = new JsonDocument();
                        r.addToProperty(SchemaOrgVocabulary.IDENTIFIER, doc.get(SchemaOrgVocabulary.IDENTIFIER));
                        r.addToProperty(HBPVocabulary.PROVENANCE_CREATED_BY, doc.get(ArangoVocabulary.CREATED_BY_USER));
                        r.addToProperty(SchemaOrgVocabulary.NAME, doc.getOrDefault(SchemaOrgVocabulary.NAME, ""));
                        r.addToProperty(SchemaOrgVocabulary.DESCRIPTION, doc.getOrDefault(SchemaOrgVocabulary.DESCRIPTION, ""));
                        result.add(r);
                    }
                }
            }
        }
        return result;
    }

    public <T> T getQueryPayload(StoredQueryReference queryReference, Class<T> clazz) {
        return arangoInternalRepository.getInternalDocumentByKey(new ArangoDocumentReference(SPECIFICATION_QUERIES, queryReference.getName()), clazz);
    }


    public QueryResult<List<Map>> metaQueryPropertyGraphByStoredSpecification(StoredQuery query) throws
            IOException, JSONException {
        return metaQueryBySpecification(resolveStoredQuery(query));
    }

    public Map queryReleaseTree(StoredQuery query, NexusInstanceReference instanceReference, TreeScope scope) throws
            IOException, JSONException {
        Query resolvedQuery;
        try {
            resolvedQuery = resolveStoredQuery(query);
        }
        catch(StoredQueryNotFoundException e){
            resolvedQuery = null;
        }
        return queryReleaseTree(resolvedQuery, instanceReference, scope);
    }


    public QueryResult<List<Map>> queryPropertyGraphByStoredSpecification(StoredQuery query) throws
            IOException, JSONException, SolrServerException {
        return queryPropertyGraphBySpecification(resolveStoredQuery(query), query.getStoredQueryReference().getAlias());
    }

    public Query resolveStoredQuery(StoredQuery storedQuery) {
        String queryPayload = getQueryPayload(storedQuery.getStoredQueryReference(), String.class);
        if(queryPayload==null){
            NexusSchemaReference organizationGlobalQuery = new NexusSchemaReference(storedQuery.getStoredQueryReference().getSchemaReference().getOrganization(), StoredQueryReference.GLOBAL_QUERY_SCHEMA.getDomain(), StoredQueryReference.GLOBAL_QUERY_SCHEMA.getSchema(), StoredQueryReference.GLOBAL_QUERY_SCHEMA.getSchemaVersion());
            StoredQueryReference organizationDefinition = new StoredQueryReference(organizationGlobalQuery, storedQuery.getStoredQueryReference().getAlias());
            queryPayload = getQueryPayload(organizationDefinition, String.class);
            if(queryPayload==null) {
                StoredQueryReference globalDefinition = new StoredQueryReference(StoredQueryReference.GLOBAL_QUERY_SCHEMA, storedQuery.getStoredQueryReference().getAlias());
                queryPayload = getQueryPayload(globalDefinition, String.class);
                if (queryPayload == null) {
                    throw new StoredQueryNotFoundException("Did not find query " + storedQuery.getStoredQueryReference().getName());
                }
            }
        }
        return new Query(storedQuery, queryPayload);
    }


    public void storeSpecificationInDb(String specification, StoredQueryReference queryReference) throws JSONException, IllegalAccessException {
        JSONObject jsonObject = new JSONObject(specification);
        JSONObject rootSchema = new JSONObject();
        rootSchema.put(JsonLdConsts.ID, nexusConfiguration.getAbsoluteUrl(queryReference.getSchemaReference()));
        jsonObject.put(GraphQueryKeys.GRAPH_QUERY_ROOT_SCHEMA.getFieldName(), rootSchema);
        String id = queryReference.getName();
        jsonObject.put(ArangoVocabulary.KEY, id);
        jsonObject.put(ArangoVocabulary.ID, id);
        jsonObject.put(SchemaOrgVocabulary.IDENTIFIER, queryReference.getSchemaReference().toString() + "/" +queryReference.getAlias());
        ArangoDocumentReference document = new ArangoDocumentReference(SPECIFICATION_QUERIES, id);
        arangoInternalRepository.insertOrUpdateDocument(document, jsonObject.toString());
    }

    public void removeSpecificationInDb(StoredQueryReference queryReference) throws IllegalAccessException {
        ArangoDocumentReference documentRef = new ArangoDocumentReference(SPECIFICATION_QUERIES, queryReference.getName());
        arangoInternalRepository.removeInternalDocument(documentRef);
    }

    public QueryResult<List<Map>> queryPropertyGraphByStoredSpecificationAndFreemarkerTemplate(StoredQuery storedQuery) throws IOException, JSONException, SolrServerException {
        QueryResult<List<Map>> queryResult = queryPropertyGraphByStoredSpecification(storedQuery);
        String templatePayload = templating.getTemplateById(storedQuery.getStoredTemplateReference()).getTemplateContent();
        String result = freemarkerTemplating.applyTemplate(templatePayload, queryResult, storedQuery.getStoredLibraryReference(), databaseFactory.getInternalDB());
        return createResult(queryResult, jsonTransformer.parseToListOfMaps(result), storedQuery.isReturnOriginalJson());
    }

    public Map queryPropertyGraphByStoredSpecificationAndStoredTemplateWithId(StoredQuery query) throws IOException, JSONException, SolrServerException {
        return queryPropertyGraphByStoredSpecificationAndTemplateWithId(query, templating.getTemplateById(query.getStoredTemplateReference()).getTemplateContent());
    }

    public Map queryPropertyGraphByStoredSpecificationAndTemplateWithId(StoredQuery query, String templatePayload) throws IOException, JSONException, SolrServerException {
        QueryResult<List<Map>> queryResult = queryPropertyGraphByStoredSpecification(query);
        if (queryResult.getResults().size() > 0) {
            String result = freemarkerTemplating.applyTemplate(templatePayload, queryResult, query.getStoredLibraryReference(), databaseFactory.getInternalDB());
            return jsonTransformer.parseToMap(result);
        }
        return null;
    }

    public QueryResult<Map> metaQueryPropertyGraphByStoredSpecificationAndFreemarkerTemplate(StoredQuery storedQuery) throws IOException, JSONException {

        Template template = templating.getTemplateById(storedQuery.getStoredTemplateReference());
        QueryResult<List<Map>> queryResult = metaQueryPropertyGraphByStoredSpecification(storedQuery);
        String result = freemarkerTemplating.applyTemplate(template.getTemplateContent(), queryResult, storedQuery.getStoredLibraryReference(), databaseFactory.getInternalDB());
        Map map = jsonTransformer.parseToMap(result);
        return createResult(queryResult, map, storedQuery.isReturnOriginalJson());
    }

    private <T> QueryResult<T> createResult(QueryResult<List<Map>> queryResult, T result, boolean addOriginalSource) {
        QueryResult<T> r;
        if(addOriginalSource){
            r = new TransformedQueryResult<>();
            ((TransformedQueryResult<T>)r).setOriginalJson(queryResult.getResults());
        }
        else{
            r = new QueryResult<>();
        }
        r.setDatabaseScope(queryContext.getDatabaseScope().name());
        r.setResults(result);
        r.setApiName(queryResult.getApiName());
        r.setTotal(queryResult.getTotal());
        r.setSize(queryResult.getSize());
        r.setStart(queryResult.getStart());
        return r;
    }

    public Set<String> getAllQueryKeys() {
        return arangoInternalRepository.getAll(SPECIFICATION_QUERIES, Map.class).stream().map(q -> (String) q.get(ArangoVocabulary.KEY)).collect(Collectors.toSet());
    }

    public List<JsonDocument> getQuery(String queryId) {
        Set<String> allQueryIds = getAllQueryKeys();
        String arangoId = ArangoNamingHelper.createCompatibleId(queryId);
        return allQueryIds.stream().filter(s -> s.endsWith("-" + arangoId)).map(s -> s.replaceAll("-" + arangoId, "")).map(
                s -> {
                    NexusSchemaReference nexusSchema = lookupMap.getNexusSchema(new ArangoCollectionReference(s));
                    JsonDocument jsonDocument = new JsonDocument();
                    jsonDocument.put(HBPVocabulary.RELATIVE_URL_OF_INTERNAL_LINK, nexusSchema.getRelativeUrl().getUrl());
                    return jsonDocument;
                }).collect(Collectors.toList());
    }

}
