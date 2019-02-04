package org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.query;

import com.arangodb.ArangoCursor;
import com.github.jsonldjava.core.JsonLdConsts;
import org.apache.commons.lang.text.StrSubstitutor;
import org.humanbrainproject.knowledgegraph.annotations.ToBeTested;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.builders.AbstractArangoQueryBuilder;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoAlias;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoCollectionReference;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.exceptions.RootCollectionNotFoundException;
import org.humanbrainproject.knowledgegraph.context.QueryContext;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusSchemaReference;
import org.humanbrainproject.knowledgegraph.query.entity.Pagination;
import org.humanbrainproject.knowledgegraph.query.entity.QueryResult;
import org.humanbrainproject.knowledgegraph.query.entity.SpecField;
import org.humanbrainproject.knowledgegraph.query.entity.SpecTraverse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.configurationprocessor.json.JSONException;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
@ToBeTested(systemTestRequired = true)
public class SpecificationQuery {

    @Autowired
    QueryContext queryContext;


    public QueryResult<List<Map>> query(AbstractArangoQueryBuilder arangoQueryBuilder) throws JSONException {
        handleEdgesAsLeaf(arangoQueryBuilder.getSpecification().fields, arangoQueryBuilder.getExistingArangoCollections());
        return query(createQuery(arangoQueryBuilder), arangoQueryBuilder.getSpecification().name, arangoQueryBuilder.getPagination());
    }

    private String createQuery(AbstractArangoQueryBuilder queryBuilder) throws JSONException {
        NexusSchemaReference nexusReference = NexusSchemaReference.createFromUrl(StrSubstitutor.replace(queryBuilder.getSpecification().rootSchema, queryContext.getAllParameters()));
        ArangoCollectionReference collection = ArangoCollectionReference.fromNexusSchemaReference(nexusReference);
        if(queryBuilder.getExistingArangoCollections()!=null && !queryBuilder.getExistingArangoCollections().contains(collection)){
            throw new RootCollectionNotFoundException(String.format("Was not able to find the root collection with the name %s", collection.getName()));
        }
        queryBuilder.addRoot(collection);
        handleFields(queryBuilder.getSpecification().fields, queryBuilder, true, false);
        String query = queryBuilder.build();
        if (queryContext.getAllParameters() != null) {
            query = StrSubstitutor.replace(query, queryContext.getAllParameters());
        }
        return query;
    }


    private Set<String> handleFields(List<SpecField> fields, AbstractArangoQueryBuilder queryBuilder, boolean isRoot, boolean isMerge) {
        Set<String> skipFields = new HashSet<>();
        SpecField originalField = queryBuilder.getCurrentField();
        for (SpecField field : fields) {
            ArangoAlias arangoField = ArangoAlias.fromSpecField(field);
            queryBuilder.setCurrentField(field);
            if (field.isMerge()) {
                Set<String> skippedMergeFields = handleFields(field.fields, queryBuilder, false, true);
                Set<ArangoAlias> skippedMergeAliases = skippedMergeFields.stream().map(ArangoAlias::fromOriginalFieldName).collect(Collectors.toSet());
                Set<ArangoAlias> mergedFields = field.fields.stream().map(ArangoAlias::fromSpecField).collect(Collectors.toSet());
                Set<ArangoAlias> nonSkippedMergeFields = mergedFields.stream().filter(f -> !skippedMergeAliases.contains(f)).collect(Collectors.toSet());
                if(!nonSkippedMergeFields.isEmpty()) {
                    queryBuilder.addMerge(arangoField, mergedFields, field.sortAlphabetically);
                }
                else{
                    skipFields.add(field.fieldName);
                }
            } else if (field.needsTraversal()) {
                SpecTraverse firstTraversal = field.getFirstTraversal();
                ArangoCollectionReference traverseCollection = ArangoCollectionReference.fromSpecTraversal(firstTraversal);
                if (queryBuilder.getExistingArangoCollections()==null || queryBuilder.getExistingArangoCollections().contains(traverseCollection)) {
                    List<ArangoAlias> groupingFields = getGroupingFields(field);
                    queryBuilder.addAlias(arangoField);
                    queryBuilder.enterTraversal(arangoField, field.numberOfDirectTraversals(), firstTraversal.reverse, traverseCollection, !groupingFields.isEmpty(), field.ensureOrder);
                    int traversalDepth=0;
                    List<SpecTraverse> traversed = new ArrayList<>();
                    for (SpecTraverse traversal : field.getAdditionalDirectTraversals()) {
                        traverseCollection = ArangoCollectionReference.fromSpecTraversal(traversal);
                        if (queryBuilder.getExistingArangoCollections() ==null || queryBuilder.getExistingArangoCollections().contains(traverseCollection)) {
                            queryBuilder.addTraversal(traversal.reverse, traverseCollection, traversalDepth++);
                            traversed.add(traversal);
                        } else {
                            skipFields.add(field.fieldName);
                        }
                    }
                    Collections.reverse(traversed);
                    boolean leaf = true;
                    for (SpecTraverse traversal : field.getAdditionalDirectTraversals()) {
                        queryBuilder.leaveAdditionalTraversal(traversal.reverse, ArangoCollectionReference.fromSpecTraversal(traversal), traversalDepth--, leaf);
                        leaf = false;
                    }
                    queryBuilder.nullFilter();
                    if (field.ensureOrder) {
                        queryBuilder.ensureOrder();
                    }
                    if (field.fields.isEmpty()) {
                        if (field.isSortAlphabetically()) {
                            queryBuilder.addSortByLeafField(Collections.singleton(ArangoAlias.fromLeafPath(field.getLeafPath())));
                        }
                        queryBuilder.addSimpleLeafResultField(ArangoAlias.fromLeafPath(field.getLeafPath()));
                    } else {
                        handleFields(field.fields, queryBuilder, false, false);
                    }
                    queryBuilder.leaveTraversal();
                    if (!groupingFields.isEmpty()) {
                        queryBuilder.buildGrouping(field.getGroupedInstances(), groupingFields, getNonGroupingFields(field));
                    }
                    queryBuilder.dropAlias();
                } else {
                    skipFields.add(field.fieldName);
                }
            } else{
                queryBuilder.addAlias(arangoField);
                queryBuilder.prepareLeafField(field);
                queryBuilder.dropAlias();
            }
        }
        queryBuilder.setCurrentField(originalField);
        if (!isMerge) {
            queryBuilder.addOrganizationFilter();
            for (SpecField field : fields) {
                if (!skipFields.contains(field.fieldName)) {
                    queryBuilder.setCurrentField(field);
                    if (field.isRequired()) {
                        if (field.needsTraversal()) {
                            queryBuilder.addTraversalFieldRequiredFilter(ArangoAlias.fromSpecField(field));
                        } else if (field.isLeaf()) {
                            queryBuilder.addComplexFieldRequiredFilter(ArangoAlias.fromLeafPath(field.getLeafPath()));
                        }
                    }
                    if (field.fieldFilter != null){
                        queryBuilder.addFieldFilter(ArangoAlias.fromLeafPath(field.getLeafPath()) );
                    }
                }
            }
            queryBuilder.setCurrentField(originalField);
            Set<ArangoAlias> sortFields = new LinkedHashSet<>();
            for (SpecField field : fields) {
                if (!skipFields.contains(field.fieldName)) {
                    queryBuilder.setCurrentField(field);
                    if (field.isLeaf() && field.isSortAlphabetically()) {
                        sortFields.add(ArangoAlias.fromLeafPath(field.getLeafPath()));
                    }
                }
            }
            queryBuilder.setCurrentField(originalField);
            if (isRoot) {
                queryBuilder.addInstanceIdFilter();
                queryBuilder.addSearchQuery();
                if (!sortFields.isEmpty()) {
                    queryBuilder.addSortByLeafField(sortFields);
                }
                queryBuilder.addLimit();
            }else{
                if (!sortFields.isEmpty()) {
                    queryBuilder.addSortByLeafField(sortFields);
                }
            }


            queryBuilder.startReturnStructure(false);
            for (SpecField field : fields) {
                if (!skipFields.contains(field.fieldName)) {
                    queryBuilder.setCurrentField(field);
                    if (field.needsTraversal()) {
                        queryBuilder.addTraversalResultField(field.fieldName, ArangoAlias.fromSpecField(field));
                    } else if (field.isLeaf()) {
                        queryBuilder.addComplexLeafResultField(field.fieldName, ArangoAlias.fromLeafPath(field.getLeafPath()));
                    }
                }
            }
            queryBuilder.setCurrentField(originalField);
            queryBuilder.endReturnStructure();
        }
        return skipFields;
    }

    private List<ArangoAlias> getGroupingFields(SpecField field) {
        return field.fields.stream().filter(SpecField::isGroupby).map(ArangoAlias::fromSpecField).collect(Collectors.toList());
    }

    private List<ArangoAlias> getNonGroupingFields(SpecField field) {
        return field.fields.stream().filter(f -> !f.isGroupby()).map(ArangoAlias::fromSpecField).collect(Collectors.toList());
    }


    private QueryResult<List<Map>> query(String aqlQuery, String apiName, Pagination pagination){
        QueryResult<List<Map>> result = new QueryResult<>();
        result.setApiName(apiName);
        if(pagination!=null) {
            result.setStart((long) pagination.getStart());
        }
        ArangoCursor<Map> cursor = queryContext.queryDatabase(aqlQuery,true, pagination, Map.class);
        result.setResults(cursor.asListRemaining());
        Long count;
        if (pagination!=null && pagination.getSize() != null) {
            count = cursor.getStats().getFullCount();
        } else {
            count = cursor.getCount().longValue();
        }
        result.setTotal(count);
        result.setSize(pagination==null || pagination.getSize() == null ? count : Math.min(count, result.getResults().size()));
        return result;
    }


    private void handleEdgesAsLeaf(List<SpecField> fields, Set<ArangoCollectionReference> existingCollections){
        for (SpecField field : fields) {
            if(field.isLeaf()){
                ArangoCollectionReference potentialCollection = ArangoCollectionReference.fromSpecTraversal(field.getLeafPath());
                if(existingCollections!=null && existingCollections.contains(potentialCollection)){
                    //The leaf is an edge collection -> we provide the default behavior
                    SpecField idField = new SpecField(JsonLdConsts.ID, null, Collections.singletonList(new SpecTraverse(JsonLdConsts.ID, false)), null, true, false, false, false, null);
                    field.fields.add(idField);
                }
            }
            else if(field.fields!=null && !field.fields.isEmpty()){
                handleEdgesAsLeaf(field.fields, existingCollections);
            }
        }
    }

}
