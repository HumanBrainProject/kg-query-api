package org.humanbrainproject.knowledgegraph.query.control;

import org.apache.solr.client.solrj.SolrServerException;
import org.humanbrainproject.knowledgegraph.annotations.Tested;
import org.humanbrainproject.knowledgegraph.commons.authorization.control.AuthorizationContext;
import org.humanbrainproject.knowledgegraph.commons.nexus.control.NexusConfiguration;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.ArangoRepository;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.builders.DataQueryBuilder;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.builders.DefaultReleaseTreeBuilder;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.builders.SpecificationBasedReleaseTreeBuilder;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.query.SpecificationQuery;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoCollectionReference;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoDocumentReference;
import org.humanbrainproject.knowledgegraph.context.QueryContext;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusInstanceReference;
import org.humanbrainproject.knowledgegraph.query.entity.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.configurationprocessor.json.JSONException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Tested
@Component
public class SpecificationController {

    @Autowired
    AuthorizationContext authorizationContext;

    @Autowired
    QueryContext queryContext;

    @Autowired
    SpecificationQuery specificationQuery;

    @Autowired
    NexusConfiguration configuration;

    @Autowired
    ArangoRepository repository;

    public QueryResult<List<Map>> metaSpecification(Specification spec) {
        JsonDocument fields = buildMetaSpecification(spec.getFields());
        JsonDocument specificationInfo = new JsonDocument();
        for (String firstLevelKey : spec.getOriginalDocument().keySet()) {
            if(!firstLevelKey.startsWith("_") && !GraphQueryKeys.isKey(firstLevelKey)){
                specificationInfo.put(firstLevelKey, spec.getOriginalDocument().get(firstLevelKey));
            }
        }
        fields.addToProperty(GraphQueryKeys.GRAPH_QUERY_SPECIFICATION.getFieldName(), specificationInfo);
        return  QueryResult.createSingleton(spec.getName(), fields);
    }

    private JsonDocument buildMetaSpecification(List<SpecField> specFields) {
        JsonDocument inner = new JsonDocument();
        for (SpecField specField : specFields) {
            JsonDocument innerFields;
            SpecField fieldToBeProcessed = specField;
            if(fieldToBeProcessed.isMerge() && fieldToBeProcessed.hasSubFields()){
                //The field specifications are duplicated during specification parsing for merged fields. It's therefore safe to just follow the first path and take it as a placeholder for the overall field.
                fieldToBeProcessed = fieldToBeProcessed.fields.get(0);
            }
            if(fieldToBeProcessed.hasSubFields()){
                if(fieldToBeProcessed.hasGrouping()){
                    innerFields = buildMetaSpecification(fieldToBeProcessed.fields.stream().filter(f -> f.isGroupby()).collect(Collectors.toList()));
                    innerFields.put(fieldToBeProcessed.groupedInstances, buildMetaSpecification(fieldToBeProcessed.fields.stream().filter(f -> !f.isGroupby()).collect(Collectors.toList())));
                }
                else {
                    innerFields = buildMetaSpecification(fieldToBeProcessed.fields);
                }
            }
            else{
                innerFields = new JsonDocument();
            }
            inner.put(specField.fieldName, innerFields);
            if(fieldToBeProcessed.customDirectives!=null) {
                for (String key : fieldToBeProcessed.customDirectives.keySet()) {
                    innerFields.put(key, fieldToBeProcessed.customDirectives.get(key));
                }
            }
        }
        return inner;
    }

    public Map releaseTreeBySpecification(Specification spec, Query query, NexusInstanceReference instanceReference) throws JSONException {
        SpecificationBasedReleaseTreeBuilder builder = new SpecificationBasedReleaseTreeBuilder(spec, authorizationContext.getReadableOrganizations(query.getFilter().getRestrictToOrganizations()), ArangoDocumentReference.fromNexusInstance(instanceReference), queryContext.getExistingCollections(), configuration.getNexusBase(NexusConfiguration.ResourceType.DATA));
        List<Map> results = specificationQuery.queryForSimpleMap(builder.build());
        if(results==null || results.isEmpty()){
            return null;
        }
        return results.get(0);
    }

    public Map defaultReleaseTree(NexusInstanceReference instanceReference){
        DefaultReleaseTreeBuilder builder = new DefaultReleaseTreeBuilder(authorizationContext.getReadableOrganizations(null), ArangoDocumentReference.fromNexusInstance(instanceReference), configuration.getNexusBase(NexusConfiguration.ResourceType.DATA));
        List<Map> results = specificationQuery.queryForSimpleMap(builder.build());
        if(results==null || results.isEmpty()){
            return null;
        }
        return results.get(0);
    }


    public QueryResult<List<Map>> queryForSpecification(Specification spec, Pagination pagination, Filter filter) throws IOException, SolrServerException {
        Set<String> readableOrganizations = authorizationContext.getReadableOrganizations(filter.getRestrictToOrganizations());
        Set<ArangoCollectionReference> existingCollections = queryContext.getExistingCollections();
        DataQueryBuilder queryBuilderNew = new DataQueryBuilder(spec, readableOrganizations, pagination, queryContext.getAllParameters(), existingCollections);
        return specificationQuery.queryForData(queryBuilderNew, filter.getRestrictToIds(), filter.getQueryString());
    }

}
