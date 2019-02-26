package org.humanbrainproject.knowledgegraph.query.control;

import org.apache.solr.client.solrj.SolrServerException;
import org.humanbrainproject.knowledgegraph.annotations.Tested;
import org.humanbrainproject.knowledgegraph.commons.authorization.control.AuthorizationContext;
import org.humanbrainproject.knowledgegraph.commons.nexus.control.NexusConfiguration;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.ArangoRepository;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.builders.ArangoReflectionQueryBuilder;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.builders.QueryBuilderNew;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.query.SpecificationQuery;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoAlias;
import org.humanbrainproject.knowledgegraph.commons.vocabulary.ArangoVocabulary;
import org.humanbrainproject.knowledgegraph.context.QueryContext;
import org.humanbrainproject.knowledgegraph.query.entity.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.configurationprocessor.json.JSONException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Map;
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
        JsonDocument fields = buildMetaSpecification(spec.fields);
        JsonDocument specificationInfo = new JsonDocument();
        for (String firstLevelKey : spec.originalDocument.keySet()) {
            if(!firstLevelKey.startsWith("_") && !GraphQueryKeys.isKey(firstLevelKey)){
                specificationInfo.put(firstLevelKey, spec.originalDocument.get(firstLevelKey));
            }
        }
        fields.addToProperty(GraphQueryKeys.GRAPH_QUERY_SPECIFICATION.getFieldName(), specificationInfo);
        return  QueryResult.createSingleton(spec.name, fields);
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


    public Map reflectSpecification(Specification spec, Query query) throws JSONException {
        QueryResult<List<Map>> result = specificationQuery.query(new ArangoReflectionQueryBuilder(spec, new ArangoAlias(ArangoVocabulary.PERMISSION_GROUP), authorizationContext.getReadableOrganizations(query.getFilter().getRestrictToOrganizations()), query.getDocumentReferenceWhitelist(), queryContext.getExistingCollections(), configuration.getNexusBase(NexusConfiguration.ResourceType.DATA)));
        if (result == null || result.getResults() == null || result.getResults().isEmpty()) {
            return null;
        } else if (result.getResults().size() == 1) {
            return result.getResults().get(0);
        } else {
            throw new RuntimeException("Queried the reflection API for a specification document but found multiple return instances.");
        }
    }

    public QueryResult<List<Map>> queryForSpecification(Specification spec, Pagination pagination, Filter filter) throws IOException, SolrServerException {
        QueryBuilderNew queryBuilderNew = new QueryBuilderNew(spec, authorizationContext.getReadableOrganizations(filter.getRestrictToOrganizations()), pagination, queryContext.getAllParameters(), queryContext.getExistingCollections());
        return specificationQuery.queryForData(queryBuilderNew, filter.getRestrictToIds(), filter.getQueryString());
    }


}
