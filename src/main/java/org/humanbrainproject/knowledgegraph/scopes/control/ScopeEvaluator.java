package org.humanbrainproject.knowledgegraph.scopes.control;

import com.arangodb.ArangoCollection;
import com.github.jsonldjava.core.JsonLdConsts;
import com.github.jsonldjava.utils.JsonUtils;
import org.humanbrainproject.knowledgegraph.commons.jsonld.control.JsonLdStandardization;
import org.humanbrainproject.knowledgegraph.commons.nexus.control.NexusConfiguration;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.ArangoDatabaseFactory;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.ArangoInternalRepository;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.builders.TreeScope;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoCollectionReference;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoDocumentReference;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusInstanceReference;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusSchemaReference;
import org.humanbrainproject.knowledgegraph.query.boundary.ArangoQuery;
import org.humanbrainproject.knowledgegraph.query.boundary.CodeGenerator;
import org.humanbrainproject.knowledgegraph.query.control.SpecificationController;
import org.humanbrainproject.knowledgegraph.query.control.SpecificationInterpreter;
import org.humanbrainproject.knowledgegraph.query.entity.Specification;
import org.humanbrainproject.knowledgegraph.query.entity.StoredQueryReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.configurationprocessor.json.JSONException;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;

@Component
public class ScopeEvaluator {


    @Autowired
    ArangoInternalRepository arangoInternalRepository;

    @Autowired
    ScopeTreeController scopeTreeController;


    public Set<String> getScope(Set<NexusInstanceReference> references, String query) {
        Set<String> ids = new HashSet<>();
        Set<NexusSchemaReference> schemasWithSpecification = arangoInternalRepository.getSchemasWithSpecification(query);
        for (NexusInstanceReference reference : references) {
            recursivelyFindScope(reference, ids, reference, query, schemasWithSpecification);
        }
        return ids;
    }


    public void recursivelyFindScope(NexusInstanceReference currentInstance, Set<String> allIds, NexusInstanceReference root, String query, Set<NexusSchemaReference> schemasWithSpec) {
        Map scopeTree = scopeTreeController.getScopeTree(currentInstance, query);
        Set<String> ids = new HashSet<>();
        collectObjectIds(scopeTree, ids);
        for (String id : ids) {
            if (!allIds.contains(id)) {
                NexusInstanceReference reference = NexusInstanceReference.createFromUrl(id);
                if(schemasWithSpec.contains(reference.getNexusSchema())){
                    if (root.equals(reference) || !reference.getNexusSchema().equals(root.getNexusSchema())) {
                        allIds.add(id);
                        recursivelyFindScope(reference, allIds, root, query, schemasWithSpec);
                    }
                }
                else{
                    allIds.add(id);
                }
            }
        }
    }

    private void collectObjectIds(Map scopeTree, Set<String> objectIds) {
        if(scopeTree!=null) {
            Object id = scopeTree.get(JsonLdConsts.ID);
            if (id instanceof String) {
                objectIds.add((String) id);
            }
            Object children = scopeTree.get("children");
            if (children instanceof List) {
                for (Object child : ((List) children)) {
                    if (child instanceof Map) {
                        collectObjectIds((Map) child, objectIds);
                    }
                }
            }
        }
    }


}
