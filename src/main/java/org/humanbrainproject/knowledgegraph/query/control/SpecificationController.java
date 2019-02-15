package org.humanbrainproject.knowledgegraph.query.control;

import org.humanbrainproject.knowledgegraph.annotations.Tested;
import org.humanbrainproject.knowledgegraph.commons.authorization.control.AuthorizationContext;
import org.humanbrainproject.knowledgegraph.commons.nexus.control.NexusConfiguration;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.ArangoRepository;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.builders.*;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.query.SpecificationQuery;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoAlias;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoDocumentReference;
import org.humanbrainproject.knowledgegraph.commons.vocabulary.ArangoVocabulary;
import org.humanbrainproject.knowledgegraph.context.QueryContext;
import org.humanbrainproject.knowledgegraph.query.entity.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.configurationprocessor.json.JSONException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;

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

    public QueryResult<List<Map>> metaSpecification(Specification spec) throws JSONException {
        return specificationQuery.query(new ArangoMetaQueryBuilder(spec, queryContext.getExistingCollections()));
    }

    public QueryResult<List<Map>> metaReflectionSpecification(Specification spec, Filter filter) throws JSONException {
        return specificationQuery.query(new ArangoMetaReflectionQueryBuilder(spec, new ArangoAlias(ArangoVocabulary.PERMISSION_GROUP), authorizationContext.getReadableOrganizations(filter.getRestrictToOrganizations()), queryContext.getExistingCollections(), configuration.getNexusBase(NexusConfiguration.ResourceType.DATA)));
    }

    public Map reflectSpecification(Specification spec, Query query) throws JSONException {
        QueryResult<List<Map>> result = specificationQuery.query(new ArangoReflectionQueryBuilder(spec, new ArangoAlias(ArangoVocabulary.PERMISSION_GROUP), authorizationContext.getReadableOrganizations(query.getFilter().getRestrictToOrganizations()), query.getDocumentReferenceWhitelist(), queryContext.getExistingCollections(), configuration.getNexusBase(NexusConfiguration.ResourceType.DATA)));
        if(result == null || result.getResults() == null || result.getResults().isEmpty()){
            return null;
        }
        else if(result.getResults().size()==1){
            return result.getResults().get(0);
        }
        else{
            throw new RuntimeException("Queried the reflection API for a specification document but found multiple return instances.");
        }
    }


    public QueryResult<List<Map>> queryBySpecification(Specification spec, Set<ArangoDocumentReference> documentReferences, Pagination pagination, Filter filter){
        QueryBuilderNew queryBuilderNew = new QueryBuilderNew(spec, authorizationContext.getReadableOrganizations(filter.getRestrictToOrganizations()));

        return null;
    }


    public QueryResult<List<Map>> queryForSpecification(Specification spec, Set<ArangoDocumentReference> documentReferences, Pagination pagination, Filter filter) throws JSONException {
        if(documentReferences!=null && documentReferences.isEmpty()){
            //We have a restriction on ids - but there is nothing allowed, we don't need to execute the query - it's already clear that the result set will be empty.
            return QueryResult.createEmptyResult();
        }
        return specificationQuery.query(new ArangoQueryBuilder(spec, pagination, filter, new ArangoAlias(ArangoVocabulary.PERMISSION_GROUP), authorizationContext.getReadableOrganizations(filter.getRestrictToOrganizations()), documentReferences, queryContext.getExistingCollections()));
    }



}
