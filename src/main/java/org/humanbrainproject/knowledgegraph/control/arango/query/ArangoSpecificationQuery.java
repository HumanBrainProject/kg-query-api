package org.humanbrainproject.knowledgegraph.control.arango.query;

import com.arangodb.entity.CollectionEntity;
import com.arangodb.model.AqlQueryOptions;
import com.github.jsonldjava.utils.JsonUtils;
import org.humanbrainproject.knowledgegraph.control.Configuration;
import org.humanbrainproject.knowledgegraph.control.arango.ArangoDriver;
import org.humanbrainproject.knowledgegraph.control.arango.ArangoNamingConvention;
import org.humanbrainproject.knowledgegraph.entity.specification.SpecField;
import org.humanbrainproject.knowledgegraph.entity.specification.SpecTraverse;
import org.humanbrainproject.knowledgegraph.entity.specification.Specification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.configurationprocessor.json.JSONException;
import org.springframework.stereotype.Component;

import java.io.IOException;
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


    public List<Object> queryForSpecification(Specification spec, Set<String> whiteListOrganizations, Integer size, Integer start) throws JSONException {
        String query = createQuery(spec, whiteListOrganizations, size, start);
        List<String> strings = arangoDriver.getOrCreateDB().query(query, null, new AqlQueryOptions(), String.class).asListRemaining();
        return strings.parallelStream().map(s -> {
            try {
                return JsonUtils.fromString(s);
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }).collect(Collectors.toList());
    }

    String createQuery(Specification spec, Set<String> whitelistOrganizations, Integer size, Integer start) throws JSONException {
        Set<String> collectionLabels = arangoDriver.getCollectionLabels();
        ArangoQueryBuilder queryBuilder = new ArangoQueryBuilder(size, start, configuration.getPermissionGroup());
        String vertexLabel = namingConvention.getVertexLabel(spec.rootSchema);
        if(collectionLabels.contains(vertexLabel)) {
            queryBuilder.addRoot(vertexLabel, whitelistOrganizations);
            handleFields(spec.fields, queryBuilder, collectionLabels);
        }
        else{
            throw new RuntimeException(String.format("Was not able to find the vertex collection with the name %s", vertexLabel));
        }
        return queryBuilder.build();
    }

    private void handleFields(List<SpecField> fields, ArangoQueryBuilder queryBuilder, Set<String> collectionLabels){
        Set<String> skipFields = new HashSet<>();
        for (SpecField field : fields) {
            if(field.needsTraversal()){
                String fieldName = namingConvention.replaceSpecialCharacters(field.fieldName);
                SpecTraverse firstTraversal = field.getFirstTraversal();
                String edgeLabel = namingConvention.getEdgeLabel(firstTraversal.pathName);
                if(collectionLabels.contains(edgeLabel)) {
                    queryBuilder.enterTraversal(namingConvention.queryKey(fieldName), field.numberOfDirectTraversals(), firstTraversal.reverse, edgeLabel);
                    for (SpecTraverse traversal : field.getAdditionalDirectTraversals()) {
                        edgeLabel = namingConvention.getEdgeLabel(traversal.pathName);
                        if(collectionLabels.contains(edgeLabel)){
                            queryBuilder.addTraversal(traversal.reverse, edgeLabel);
                        }
                        else{
                            skipFields.add(field.fieldName);
                        }
                    }
                    if (field.fields.isEmpty()) {
                        queryBuilder.addSimpleLeafResultField(field.getLeafPath().pathName);
                    } else {
                        handleFields(field.fields, queryBuilder, collectionLabels);
                    }
                    queryBuilder.leaveTraversal();
                }
                else{
                    skipFields.add(field.fieldName);
                }
            }
        }
        queryBuilder.startReturnStructure(false);
        for (SpecField field : fields) {
            if(!skipFields.contains(field.fieldName)) {
                if (field.needsTraversal()) {
                    queryBuilder.addTraversalResultField(field.fieldName, namingConvention.queryKey(namingConvention.replaceSpecialCharacters(field.fieldName)));
                } else if (field.isLeaf()) {
                    queryBuilder.addComplexLeafResultField(field.fieldName, field.getLeafPath().pathName);
                }
            }
        }
        queryBuilder.endReturnStructure();
    }

}
