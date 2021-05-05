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

import com.github.jsonldjava.utils.JsonUtils;
import org.humanbrainproject.knowledgegraph.commons.jsonld.control.JsonLdStandardization;
import org.humanbrainproject.knowledgegraph.commons.nexus.control.NexusConfiguration;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.ArangoInternalRepository;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.builders.TreeScope;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoDocumentReference;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusInstanceReference;
import org.humanbrainproject.knowledgegraph.query.boundary.ArangoQuery;
import org.humanbrainproject.knowledgegraph.query.control.SpecificationController;
import org.humanbrainproject.knowledgegraph.query.control.SpecificationInterpreter;
import org.humanbrainproject.knowledgegraph.query.entity.Specification;
import org.humanbrainproject.knowledgegraph.query.entity.StoredQueryReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.configurationprocessor.json.JSONException;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

@Component
public class ScopeTreeController {


    private static Logger logger = LoggerFactory.getLogger(ScopeTreeController.class);

    @Autowired
    JsonLdStandardization standardization;

    @Autowired
    ArangoInternalRepository arangoInternalRepository;


    @Autowired
    SpecificationInterpreter specInterpreter;

    @Autowired
    SpecificationController specificationController;

    @Autowired
    NexusConfiguration nexusConfiguration;

    @Cacheable("scopeTree")
    public Map getScopeTree(NexusInstanceReference instanceReference, String queryId) {
        logger.info(String.format("Finding scope for %s with the query %s", instanceReference.getRelativeUrl().getUrl(), queryId));
        StoredQueryReference queryReference = new StoredQueryReference(instanceReference.getNexusSchema(), queryId);
        String payload = arangoInternalRepository.getInternalDocumentByKey(new ArangoDocumentReference(ArangoQuery.SPECIFICATION_QUERIES, queryReference.getName()), String.class);
        if (payload != null) {
            try {
                Specification specification = specInterpreter.readSpecification(JsonUtils.toString(standardization.fullyQualify(payload)), nexusConfiguration.getAbsoluteUrl(instanceReference.getNexusSchema()), null);
                return specificationController.scopeTreeBySpecification(specification, null, instanceReference, TreeScope.ALL);
            } catch (IOException | JSONException e) {
                e.printStackTrace();
            }
        }
        return Collections.emptyMap();

    }


    @CacheEvict(allEntries = true, cacheNames = "scopeTree")
    @Scheduled(fixedDelay = 30 * 60 * 1000)
    public void wipeScopeTree() {
    }
}
