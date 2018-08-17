package org.humanbrainproject.knowledgegraph.control.arango.query;

import com.arangodb.ArangoCursor;
import com.arangodb.model.AqlQueryOptions;
import com.google.gson.Gson;
import org.humanbrainproject.knowledgegraph.control.Configuration;
import org.humanbrainproject.knowledgegraph.control.arango.ArangoDriver;
import org.humanbrainproject.knowledgegraph.control.arango.ArangoNamingConvention;
import org.humanbrainproject.knowledgegraph.entity.query.QueryResult;
import org.humanbrainproject.knowledgegraph.entity.specification.SpecField;
import org.humanbrainproject.knowledgegraph.entity.specification.SpecTraverse;
import org.humanbrainproject.knowledgegraph.entity.specification.Specification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.configurationprocessor.json.JSONException;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class ArangoSpecificationQuery {

    @Autowired
    ArangoNamingConvention namingConvention;

    @Autowired
    @Qualifier("default")
    ArangoDriver arangoDriver;

    @Autowired
    Configuration configuration;


    public QueryResult<List<Map>> reflectSpecification(Specification spec, Set<String> whiteListOrganizations, Integer size, Integer start) throws JSONException {
        QueryResult<List<Map>> result = new QueryResult<>();
        String query = createQuery(new ArangoReflectQueryBuilder(spec, size, start, configuration.getPermissionGroup(), whiteListOrganizations));
        ArangoCursor<Map> cursor = arangoDriver.getOrCreateDB().query(query, null, new AqlQueryOptions(), Map.class);
        result.setResults(cursor.asListRemaining());
        result.setApiName(spec.name);
        return result;
    }

    public QueryResult<List<Map>> metaSpecification(Specification spec) throws JSONException {
        QueryResult<List<Map>> result = new QueryResult<>();
        String query = createQuery(new ArangoMetaQueryBuilder(spec));
        ArangoCursor<Map> cursor = arangoDriver.getOrCreateDB().query(query, null, new AqlQueryOptions(), Map.class);
        result.setResults(cursor.asListRemaining());
        result.setApiName(spec.name);
        return result;
    }

    public QueryResult<List<Map>> queryForSpecification(Specification spec, Set<String> whiteListOrganizations, Integer size, Integer start) throws JSONException {
        QueryResult<List<Map>> result = new QueryResult<>();
        String query = createQuery(new ArangoQueryBuilder(spec, size, start, configuration.getPermissionGroup(), whiteListOrganizations));
        AqlQueryOptions options = new AqlQueryOptions();
        if(size!=null) {
            options.fullCount(true);
        }
        else{
            options.count(true);
        }
        ArangoCursor<Map> cursor = arangoDriver.getOrCreateDB().query(query, null, options, Map.class);
        Long count;
        if(size!=null) {
            count = cursor.getStats().getFullCount();
        }
        else{
            count = cursor.getCount().longValue();
        }
        result.setResults(cursor.asListRemaining());
        result.setTotal(count);
        result.setApiName(spec.name);
        result.setSize(size==null ? count : size);
        result.setStart(start!=null ? start : 0L);
        return result;
    }

    String createQuery( AbstractQueryBuilder queryBuilder) throws JSONException {
        Set<String> collectionLabels = arangoDriver.getCollectionLabels();
        String vertexLabel = namingConvention.getVertexLabel(queryBuilder.getSpecification().rootSchema);
        if (collectionLabels.contains(vertexLabel)) {
            queryBuilder.addRoot(vertexLabel);
            handleFields(queryBuilder.getSpecification().fields, queryBuilder, collectionLabels, true, false);
        } else {
            throw new RuntimeException(String.format("Was not able to find the vertex collection with the name %s", vertexLabel));
        }
        return queryBuilder.build();
    }

    private List<String> getGroupingFields(SpecField field){
        return field.fields.stream().filter(SpecField::isGroupby).map(f -> f.fieldName).collect(Collectors.toList());
    }

    private List<String> getNonGroupingFields(SpecField field){
        return field.fields.stream().filter(f -> !f.isGroupby()).map(f -> f.fieldName).collect(Collectors.toList());
    }

    private void handleFields(List<SpecField> fields, AbstractQueryBuilder queryBuilder, Set<String> collectionLabels, boolean isRoot, boolean isMerge) {
        Set<String> skipFields = new HashSet<>();
        SpecField originalField = queryBuilder.currentField;
        for (SpecField field : fields) {
            queryBuilder.setCurrentField(field);
            if(field.isMerge()){
                handleFields(field.fields, queryBuilder, collectionLabels, false, true);
                Set<String> mergedFields = field.fields.stream().map(f -> namingConvention.queryKey(namingConvention.replaceSpecialCharacters(f.fieldName))).collect(Collectors.toSet());
                queryBuilder.addMerge(namingConvention.queryKey(namingConvention.replaceSpecialCharacters(field.fieldName)), mergedFields, field.sortAlphabetically);
            }
            else if (field.needsTraversal()) {
                String fieldName = namingConvention.replaceSpecialCharacters(field.fieldName);
                SpecTraverse firstTraversal = field.getFirstTraversal();
                String edgeLabel = namingConvention.getEdgeLabel(firstTraversal.pathName);
                if (collectionLabels.contains(edgeLabel)) {
                    List<String> groupingFields = getGroupingFields(field);
                    queryBuilder.addAlias(namingConvention.queryKey(fieldName));
                    queryBuilder.enterTraversal(namingConvention.queryKey(fieldName), field.numberOfDirectTraversals(), firstTraversal.reverse, edgeLabel, !groupingFields.isEmpty(), field.ensureOrder);
                    for (SpecTraverse traversal : field.getAdditionalDirectTraversals()) {
                        edgeLabel = namingConvention.getEdgeLabel(traversal.pathName);
                        if (collectionLabels.contains(edgeLabel)) {
                            queryBuilder.addTraversal(traversal.reverse, edgeLabel);
                        } else {
                            skipFields.add(field.fieldName);
                        }
                    }
                    if(field.ensureOrder){
                        queryBuilder.ensureOrder();
                    }
                    if (field.fields.isEmpty()) {
                        if (field.isSortAlphabetically()){
                            queryBuilder.addSortByLeafField(Collections.singleton(field.getLeafPath().pathName));
                        }
                        queryBuilder.addSimpleLeafResultField(field.getLeafPath().pathName);
                    } else {
                        handleFields(field.fields, queryBuilder, collectionLabels, false, false);
                    }
                    queryBuilder.leaveTraversal();
                    if(!groupingFields.isEmpty()){
                        queryBuilder.buildGrouping(field.getGroupedInstances(), groupingFields, getNonGroupingFields(field));
                    }
                    queryBuilder.dropAlias();
                } else {
                    skipFields.add(field.fieldName);
                }
            }
        }
        queryBuilder.setCurrentField(originalField);
        if(!isMerge) {
            queryBuilder.addOrganizationFilter();
            for (SpecField field : fields) {
                if (!skipFields.contains(field.fieldName)) {
                    queryBuilder.setCurrentField(field);
                    if (field.isRequired()) {
                        if (field.needsTraversal()) {
                            queryBuilder.addTraversalFieldRequiredFilter(namingConvention.queryKey(namingConvention.replaceSpecialCharacters(field.fieldName)));
                        } else if (field.isLeaf()) {
                            queryBuilder.addComplexFieldRequiredFilter(field.getLeafPath().pathName);
                        }
                    }
                }
            }
            queryBuilder.setCurrentField(originalField);
            if (isRoot) {
                queryBuilder.addLimit();
            }

            Set<String> sortFields = new LinkedHashSet<>();
            for (SpecField field : fields) {
                if (!skipFields.contains(field.fieldName)) {
                    queryBuilder.setCurrentField(field);
                    if (field.isLeaf() && field.isSortAlphabetically()) {
                        sortFields.add(field.getLeafPath().pathName);
                    }
                }
            }
            queryBuilder.setCurrentField(originalField);
            if (!sortFields.isEmpty()) {
                queryBuilder.addSortByLeafField(sortFields);
            }

            queryBuilder.startReturnStructure(false);
            for (SpecField field : fields) {
                if (!skipFields.contains(field.fieldName)) {
                    queryBuilder.setCurrentField(field);
                    if (field.needsTraversal()) {
                        queryBuilder.addTraversalResultField(field.fieldName, namingConvention.queryKey(namingConvention.replaceSpecialCharacters(field.fieldName)));
                    } else if (field.isLeaf()) {
                        queryBuilder.addComplexLeafResultField(field.fieldName, field.getLeafPath().pathName);
                    }
                }
            }
            queryBuilder.setCurrentField(originalField);
            queryBuilder.endReturnStructure();
        }

    }

}
