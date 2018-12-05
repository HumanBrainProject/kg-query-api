package org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.query;

import com.arangodb.ArangoCursor;
import com.arangodb.model.AqlQueryOptions;
import org.apache.commons.text.StrSubstitutor;
import org.humanbrainproject.knowledgegraph.commons.authorization.control.AuthorizationController;
import org.humanbrainproject.knowledgegraph.commons.authorization.entity.Credential;
import org.humanbrainproject.knowledgegraph.commons.nexus.control.NexusConfiguration;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.ArangoDatabaseFactory;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoAlias;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoCollectionReference;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoDocumentReference;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.exceptions.RootCollectionNotFoundException;
import org.humanbrainproject.knowledgegraph.commons.vocabulary.ArangoVocabulary;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusSchemaReference;
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

    @Autowired
    AuthorizationController authorizationController;


    public QueryResult<List<Map>> metaSpecification(Specification spec, QueryParameters parameters) throws JSONException {
        QueryResult<List<Map>> result = new QueryResult<>();
        String query = createQuery(new ArangoMetaQueryBuilder(spec), parameters);
        ArangoCursor<Map> cursor = databaseFactory.getConnection(parameters.databaseScope()).getOrCreateDB().query(query, null, new AqlQueryOptions(), Map.class);
        result.setResults(cursor.asListRemaining());
        result.setApiName(spec.name);
        return result;
    }

    public QueryResult<List<Map>> queryForSpecification(Specification spec, QueryParameters parameters, ArangoDocumentReference documentReference, Credential credential) throws JSONException {
        QueryResult<List<Map>> result = new QueryResult<>();
        ArangoQueryBuilder queryBuilder = new ArangoQueryBuilder(spec, parameters.pagination(), parameters.filter(), new ArangoAlias(ArangoVocabulary.PERMISSION_GROUP), authorizationController.getReadableOrganizations(credential, parameters.filter().getRestrictToOrganizations()), documentReference, databaseFactory.getConnection(parameters.databaseScope()).getCollections());
        String query = createQuery(queryBuilder, parameters);
        AqlQueryOptions options = new AqlQueryOptions();
        if (parameters.pagination().getSize() != null) {
            options.fullCount(true);
        } else {
            options.count(true);
        }
        ArangoCursor<Map> cursor = databaseFactory.getConnection(parameters.databaseScope()).getOrCreateDB().query(query, null, options, Map.class);
        Long count;
        if (parameters.pagination().getSize() != null) {
            count = cursor.getStats().getFullCount();
        } else {
            count = cursor.getCount().longValue();
        }
        result.setResults(cursor.asListRemaining());
        result.setTotal(count);
        result.setApiName(spec.name);
        result.setSize(parameters.pagination().getSize() == null ? count : parameters.pagination().getSize());
        result.setStart(parameters.pagination().getStart() != null ? parameters.pagination().getStart() : 0L);
        return result;
    }

    String createQuery(AbstractArangoQueryBuilder queryBuilder, QueryParameters parameters) throws JSONException {
        NexusSchemaReference nexusReference = NexusSchemaReference.createFromUrl(StrSubstitutor.replace(queryBuilder.getSpecification().rootSchema, parameters.getAllParameters()));
        ArangoCollectionReference collection = ArangoCollectionReference.fromNexusSchemaReference(nexusReference);
        if(queryBuilder.getExistingArangoCollections()!=null && !queryBuilder.getExistingArangoCollections().contains(collection)){
            throw new RootCollectionNotFoundException(String.format("Was not able to find the root collection with the name %s", collection.getName()));
        }
        queryBuilder.addRoot(collection);
        handleFields(queryBuilder.getSpecification().fields, queryBuilder, true, false);
        String query = queryBuilder.build();
        if (parameters.getAllParameters() != null) {
            query = StrSubstitutor.replace(query, parameters.getAllParameters());
        }
        return query;
    }

    private List<ArangoAlias> getGroupingFields(SpecField field) {
        return field.fields.stream().filter(SpecField::isGroupby).map(ArangoAlias::fromSpecField).collect(Collectors.toList());
    }

    private List<ArangoAlias> getNonGroupingFields(SpecField field) {
        return field.fields.stream().filter(f -> !f.isGroupby()).map(ArangoAlias::fromSpecField).collect(Collectors.toList());
    }

    private Set<String> handleFields(List<SpecField> fields, AbstractArangoQueryBuilder queryBuilder, boolean isRoot, boolean isMerge) {
        Set<String> skipFields = new HashSet<>();
        SpecField originalField = queryBuilder.currentField;
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
                    for (SpecTraverse traversal : field.getAdditionalDirectTraversals()) {
                        traverseCollection = ArangoCollectionReference.fromSpecTraversal(traversal);
                        if (queryBuilder.getExistingArangoCollections() ==null || queryBuilder.getExistingArangoCollections().contains(traverseCollection)) {
                            queryBuilder.addTraversal(traversal.reverse, traverseCollection);
                        } else {
                            skipFields.add(field.fieldName);
                        }
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
                }
            }
            queryBuilder.setCurrentField(originalField);
            if (isRoot) {
                if (queryBuilder.documentReference != null) {
                    queryBuilder.addInstanceIdFilter();
                }
                queryBuilder.addSearchQuery();
                queryBuilder.addLimit();
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
        return skipFields;
    }

}
