package org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.query;

import com.arangodb.ArangoCursor;
import com.arangodb.model.AqlQueryOptions;
import org.apache.commons.text.StrSubstitutor;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusSchemaReference;
import org.humanbrainproject.knowledgegraph.commons.nexus.control.NexusConfiguration;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.ArangoDatabaseFactory;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoAlias;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoCollectionReference;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoDocumentReference;
import org.humanbrainproject.knowledgegraph.query.entity.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.configurationprocessor.json.JSONException;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class ArangoSpecificationQuery {


    @Autowired
    ArangoDatabaseFactory databaseFactory;

    @Autowired
    NexusConfiguration configuration;


    public QueryResult<List<Map>> metaSpecification(Specification spec, QueryParameters parameters) throws JSONException {
        QueryResult<List<Map>> result = new QueryResult<>();
        String query = createQuery(new ArangoMetaQueryBuilder(spec), parameters);
        ArangoCursor<Map> cursor = databaseFactory.getConnection(parameters.databaseScope()).getOrCreateDB().query(query, null, new AqlQueryOptions(), Map.class);
        result.setResults(cursor.asListRemaining());
        result.setApiName(spec.name);
        return result;
    }

    public QueryResult<List<Map>> queryForSpecification(Specification spec, Set<String> whiteListOrganizations, QueryParameters parameters, ArangoDocumentReference documentReference) throws JSONException {
        QueryResult<List<Map>> result = new QueryResult<>();
        ArangoQueryBuilder queryBuilder = new ArangoQueryBuilder(spec, parameters.pagination(), parameters.filter(), new ArangoAlias(configuration.getPermissionGroup()), whiteListOrganizations, documentReference);
        String query = createQuery(queryBuilder, parameters);
        AqlQueryOptions options = new AqlQueryOptions();
        if(parameters.pagination().getSize()!=null) {
            options.fullCount(true);
        }
        else{
            options.count(true);
        }
        ArangoCursor<Map> cursor = databaseFactory.getConnection(parameters.databaseScope()).getOrCreateDB().query(query, null, options, Map.class);
        Long count;
        if(parameters.pagination().getSize()!=null) {
            count = cursor.getStats().getFullCount();
        }
        else{
            count = cursor.getCount().longValue();
        }
        result.setResults(cursor.asListRemaining());
        result.setTotal(count);
        result.setApiName(spec.name);
        result.setSize(parameters.pagination().getSize()==null ? count : parameters.pagination().getSize());
        result.setStart(parameters.pagination().getStart()!=null ? parameters.pagination().getStart() : 0L);
        return result;
    }

    String createQuery(AbstractArangoQueryBuilder queryBuilder, QueryParameters parameters) throws JSONException {
        Set<ArangoCollectionReference> existingCollections = databaseFactory.getConnection(parameters.databaseScope()).getCollections();
        NexusSchemaReference nexusReference = NexusSchemaReference.createFromUrl(StrSubstitutor.replace(queryBuilder.getSpecification().rootSchema, parameters.getAllParameters()));
        ArangoCollectionReference collection = ArangoCollectionReference.fromNexusSchemaReference(nexusReference);
        if (existingCollections.contains(collection)) {
            queryBuilder.addRoot(collection);
            handleFields(queryBuilder.getSpecification().fields, queryBuilder, existingCollections, true, false);
        } else {
            throw new RuntimeException(String.format("Was not able to find the vertex collection with the name %s", collection.getName()));
        }
        String query = queryBuilder.build();
        if(parameters.getAllParameters()!=null) {
            query = StrSubstitutor.replace(query, parameters.getAllParameters());
        }
        return query;
    }

    private List<ArangoAlias> getGroupingFields(SpecField field){
        return field.fields.stream().filter(SpecField::isGroupby).map(ArangoAlias::fromSpecField).collect(Collectors.toList());
    }

    private List<ArangoAlias> getNonGroupingFields(SpecField field){
        return field.fields.stream().filter(f -> !f.isGroupby()).map(ArangoAlias::fromSpecField).collect(Collectors.toList());
    }

    private void handleFields(List<SpecField> fields, AbstractArangoQueryBuilder queryBuilder, Set<ArangoCollectionReference> existingCollections, boolean isRoot, boolean isMerge) {
        Set<String> skipFields = new HashSet<>();
        SpecField originalField = queryBuilder.currentField;
        for (SpecField field : fields) {
            ArangoAlias arangoField = ArangoAlias.fromSpecField(field);
            queryBuilder.setCurrentField(field);
            if(field.isMerge()){
                handleFields(field.fields, queryBuilder, existingCollections, false, true);
                Set<ArangoAlias> mergedFields = field.fields.stream().map(ArangoAlias::fromSpecField).collect(Collectors.toSet());
                queryBuilder.addMerge(arangoField, mergedFields, field.sortAlphabetically);
            }
            else if (field.needsTraversal()) {
                SpecTraverse firstTraversal = field.getFirstTraversal();
                ArangoCollectionReference traverseCollection = ArangoCollectionReference.fromSpecTraversal(firstTraversal, existingCollections);
                if (traverseCollection!=null) {
                    List<ArangoAlias> groupingFields = getGroupingFields(field);
                    queryBuilder.addAlias(arangoField);
                    queryBuilder.enterTraversal(arangoField, field.numberOfDirectTraversals(), firstTraversal.reverse, traverseCollection, !groupingFields.isEmpty(), field.ensureOrder);
                    for (SpecTraverse traversal : field.getAdditionalDirectTraversals()) {
                        traverseCollection = ArangoCollectionReference.fromSpecTraversal(traversal, existingCollections);
                        if (traverseCollection!=null) {
                            queryBuilder.addTraversal(traversal.reverse, traverseCollection);
                        } else {
                            skipFields.add(field.fieldName);
                        }
                    }
                    queryBuilder.nullFilter();
                    if(field.ensureOrder){
                        queryBuilder.ensureOrder();
                    }
                    if (field.fields.isEmpty()) {
                        if (field.isSortAlphabetically()){
                            queryBuilder.addSortByLeafField(Collections.singleton(ArangoAlias.fromLeafPath(field.getLeafPath())));
                        }
                        queryBuilder.addSimpleLeafResultField(ArangoAlias.fromLeafPath(field.getLeafPath()));
                    } else {
                        handleFields(field.fields, queryBuilder, existingCollections, false, false);
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
                            queryBuilder.addTraversalFieldRequiredFilter(ArangoAlias.fromSpecField(field));
                        } else if (field.isLeaf()) {
                            queryBuilder.addComplexFieldRequiredFilter(ArangoAlias.fromLeafPath(field.getLeafPath()));
                        }
                    }
                }
            }
            queryBuilder.setCurrentField(originalField);
            if (isRoot) {
                queryBuilder.addLimit();
                if(queryBuilder.documentReference != null){
                    queryBuilder.addInstanceIdFilter();
                }
                queryBuilder.addSearchQuery();
            }

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
            if (!sortFields.isEmpty()) {
                queryBuilder.addSortByLeafField(sortFields);
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

    }

}
