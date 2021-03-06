/*
 * Copyright 2018 - 2021 Swiss Federal Institute of Technology Lausanne (EPFL)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * This open source software code was developed in part or in whole in the
 * Human Brain Project, funded from the European Union's Horizon 2020
 * Framework Programme for Research and Innovation under
 * Specific Grant Agreements No. 720270, No. 785907, and No. 945539
 * (Human Brain Project SGA1, SGA2 and SGA3).
 *
 */

package org.humanbrainproject.knowledgegraph.query.control;

import org.apache.solr.client.solrj.SolrServerException;
import org.humanbrainproject.knowledgegraph.annotations.Tested;
import org.humanbrainproject.knowledgegraph.commons.authorization.control.AuthorizationContext;
import org.humanbrainproject.knowledgegraph.commons.authorization.entity.InternalMasterKey;
import org.humanbrainproject.knowledgegraph.commons.nexus.control.NexusConfiguration;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.ArangoRepository;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.builders.*;
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
        return QueryResult.createSingleton(spec.getName(), fields, queryContext.getDatabaseScope().name());
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


    public Map scopeTreeBySpecification(Specification spec, Query query, NexusInstanceReference instanceReference, TreeScope scope){
        return getTreeBySpecification(new SpecificationBasedScopeBuilder(spec, authorizationContext.getReadableOrganizations(new InternalMasterKey(), query!=null ? query.getFilter().getRestrictToOrganizations() : null), ArangoDocumentReference.fromNexusInstance(instanceReference), queryContext.getExistingCollections(), configuration.getNexusBase(NexusConfiguration.ResourceType.DATA), scope));
    }

    private Map getTreeBySpecification(SpecificationBasedScopeBuilder builder){
        List<Map> results = specificationQuery.queryForSimpleMap(builder.build());
        if(results==null || results.isEmpty()){
            return null;
        }
        return results.get(0);
    }

    public Map releaseTreeBySpecification(Specification spec, Query query, NexusInstanceReference instanceReference, TreeScope scope) throws JSONException {
        return getTreeBySpecification(new SpecificationBasedReleaseTreeBuilder(spec, authorizationContext.getReadableOrganizations(query.getFilter().getRestrictToOrganizations()), ArangoDocumentReference.fromNexusInstance(instanceReference), queryContext.getExistingCollections(), configuration.getNexusBase(NexusConfiguration.ResourceType.DATA), scope));
    }

    public Map defaultReleaseTree(NexusInstanceReference instanceReference){
        DefaultReleaseTreeBuilder builder = new DefaultReleaseTreeBuilder(authorizationContext.getReadableOrganizations(null), ArangoDocumentReference.fromNexusInstance(instanceReference), configuration.getNexusBase(NexusConfiguration.ResourceType.DATA));
        List<Map> results = specificationQuery.queryForSimpleMap(builder.build());
        if(results==null || results.isEmpty()){
            return null;
        }
        return results.get(0);
    }


    public QueryResult<List<Map>> queryForSpecification(Specification spec, Pagination pagination, Filter filter, String queryName) throws IOException, SolrServerException {
        Set<String> readableOrganizations = authorizationContext.getReadableOrganizations(filter.getRestrictToOrganizations());
        Set<String> invitations = queryName!=null ? authorizationContext.getInvitations(queryName) : null;

        Set<ArangoCollectionReference> existingCollections = queryContext.getExistingCollections();
        DataQueryBuilder queryBuilderNew = new DataQueryBuilder(spec, readableOrganizations, invitations, pagination, queryContext.getAllParameters(), existingCollections);
        return specificationQuery.queryForData(queryBuilderNew, filter.getRestrictToIds(), filter.getQueryString());
    }

}
