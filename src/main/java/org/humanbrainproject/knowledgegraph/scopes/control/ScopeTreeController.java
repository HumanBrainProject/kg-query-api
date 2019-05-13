package org.humanbrainproject.knowledgegraph.scopes.control;

import com.github.jsonldjava.utils.JsonUtils;
import org.humanbrainproject.knowledgegraph.commons.jsonld.control.JsonLdStandardization;
import org.humanbrainproject.knowledgegraph.commons.nexus.control.NexusConfiguration;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.ArangoInternalRepository;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.builders.TreeScope;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoDocumentReference;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusInstanceReference;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusSchemaReference;
import org.humanbrainproject.knowledgegraph.query.boundary.ArangoQuery;
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
import java.util.Collections;
import java.util.Map;

@Component
public class ScopeTreeController {

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
        System.out.println(String.format("Finding scope for %s with the query %s", instanceReference.getRelativeUrl().getUrl(), queryId));
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
