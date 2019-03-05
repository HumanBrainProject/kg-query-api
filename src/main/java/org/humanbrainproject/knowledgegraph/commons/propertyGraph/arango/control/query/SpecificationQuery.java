package org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.query;

import com.arangodb.ArangoCursor;
import com.github.jsonldjava.core.JsonLdConsts;
import org.apache.solr.client.solrj.SolrServerException;
import org.humanbrainproject.knowledgegraph.annotations.ToBeTested;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.builders.DataQueryBuilder;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoCollectionReference;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoDocumentReference;
import org.humanbrainproject.knowledgegraph.context.QueryContext;
import org.humanbrainproject.knowledgegraph.query.control.SpatialSearch;
import org.humanbrainproject.knowledgegraph.query.entity.*;
import org.humanbrainproject.knowledgegraph.query.entity.fieldFilter.Op;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@ToBeTested(systemTestRequired = true)
public class SpecificationQuery {

    @Autowired
    QueryContext queryContext;

    @Autowired
    SpatialSearch spatialSearch;


    public QueryResult<List<Map>> queryForData(DataQueryBuilder queryBuilderNew, List<String> restrictedIds, String search) throws IOException, SolrServerException {
        if(!queryBuilderNew.existsRootSchema()){
            return QueryResult.createEmptyResult();
        }
        String query = queryBuilderNew.build(restrictedIds, search);
        Map<String, Object> filterValues = queryBuilderNew.getProcessedFilterValues();

        //Resolve spatial search
        Specification spec = queryBuilderNew.getSpecification();
        List<String> definedMbbParameters = spec.getAllFilterParameters().stream().filter(f -> f.getOperation().equals(Op.MBB.getName())).map(f -> f.getParameterName()).filter(k -> filterValues.containsKey(k)).collect(Collectors.toList());
        for (String definedMbbParameter : definedMbbParameters) {
            Object boundingBox = filterValues.get(definedMbbParameter);
            if(boundingBox instanceof String){
                BoundingBox bbox = BoundingBox.parseBoundingBox((String) boundingBox);
                Set<ArangoDocumentReference> arangoDocumentReferences = spatialSearch.minimalBoundingBox(bbox);
                List<String> idRestrictions = DataQueryBuilder.createIdRestriction(arangoDocumentReferences);
                filterValues.put(definedMbbParameter, idRestrictions);
            }
        }
        return query(query, spec.getName(), queryBuilderNew.getPagination(), filterValues);
    }

    public QueryResult<List<Map>> query(String aqlQuery, String apiName, Pagination pagination){
        return query(aqlQuery, apiName, pagination, null);
    }

    public List<Map> queryForSimpleMap(String aqlQuery){
        return queryContext.queryDatabase(aqlQuery, false, null, Map.class, null).asListRemaining();
    }


    public QueryResult<List<Map>> query(String aqlQuery, String apiName, Pagination pagination, Map<String, Object> bindParameters){
        QueryResult<List<Map>> result = new QueryResult<>();
        result.setApiName(apiName);
        if(pagination!=null) {
            result.setStart((long) pagination.getStart());
        }
        ArangoCursor<Map> cursor = queryContext.queryDatabase(aqlQuery,true, pagination, Map.class, bindParameters);
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
