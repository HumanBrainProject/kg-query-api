package org.humanbrainproject.knowledgegraph.nexusExt.control;

import com.github.jsonldjava.core.JsonLdConsts;
import org.humanbrainproject.knowledgegraph.commons.authorization.entity.OidcAccessToken;
import org.humanbrainproject.knowledgegraph.commons.jsonld.control.JsonTransformer;
import org.humanbrainproject.knowledgegraph.commons.nexus.control.NexusClient;
import org.humanbrainproject.knowledgegraph.commons.nexus.control.NexusConfiguration;
import org.humanbrainproject.knowledgegraph.commons.vocabulary.HBPVocabulary;
import org.humanbrainproject.knowledgegraph.indexing.control.nexusToArango.NexusToArangoIndexingProvider;
import org.humanbrainproject.knowledgegraph.indexing.entity.IndexingMessage;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusInstanceReference;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusSchemaReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@Component
public class NexusReleasingController {

    @Autowired
    NexusClient nexusClient;

    @Autowired
    InstanceController instanceController;

    @Autowired
    NexusConfiguration configuration;

    @Autowired
    JsonTransformer jsonTransformer;

    @Autowired
    NexusToArangoIndexingProvider nexusToArangoIndexingProvider;

    public IndexingMessage release(NexusInstanceReference instanceReference, Integer revision, OidcAccessToken oidcAccessToken) {
        Map<String, Object> payload = new LinkedHashMap<>();
        Map<String, Object> reference = new HashMap<>();
        reference.put(JsonLdConsts.ID, configuration.getAbsoluteUrl(instanceReference));
        payload.put(HBPVocabulary.RELEASE_REVISION, revision);
        payload.put(HBPVocabulary.RELEASE_INSTANCE, reference);
        payload.put(JsonLdConsts.TYPE, HBPVocabulary.RELEASE_TYPE);
        NexusSchemaReference releaseSchema = new NexusSchemaReference(instanceReference.getNexusSchema().getOrganization(), "prov", "release", "v0.0.2");
        NexusInstanceReference instance = instanceController.createInstanceByIdentifier(releaseSchema, instanceReference.getFullId(false), payload, oidcAccessToken);
        return new IndexingMessage(instance, jsonTransformer.getMapAsJson(payload), null, null);
    }


    public Set<NexusInstanceReference> unrelease(NexusInstanceReference instanceReference, OidcAccessToken oidcAccessToken) {
        //Find release instance
        Set<NexusInstanceReference> releases = nexusToArangoIndexingProvider.findInstancesWithLinkTo(HBPVocabulary.RELEASE_INSTANCE, instanceReference);
        for (NexusInstanceReference nexusInstanceReference : releases) {
            nexusClient.delete(nexusInstanceReference.getRelativeUrl(), nexusInstanceReference.getRevision() != null ? nexusInstanceReference.getRevision() : 1, oidcAccessToken);
        }
        return releases;
    }

}
