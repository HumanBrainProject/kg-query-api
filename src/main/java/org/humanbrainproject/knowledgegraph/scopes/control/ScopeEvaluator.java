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
