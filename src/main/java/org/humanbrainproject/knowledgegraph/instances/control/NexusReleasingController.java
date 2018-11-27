package org.humanbrainproject.knowledgegraph.instances.control;

import org.humanbrainproject.knowledgegraph.commons.authorization.entity.Credential;
import org.humanbrainproject.knowledgegraph.commons.jsonld.control.JsonTransformer;
import org.humanbrainproject.knowledgegraph.commons.nexus.control.NexusClient;
import org.humanbrainproject.knowledgegraph.commons.nexus.control.NexusConfiguration;
import org.humanbrainproject.knowledgegraph.commons.vocabulary.HBPVocabulary;
import org.humanbrainproject.knowledgegraph.indexing.control.nexusToArango.NexusToArangoIndexingProvider;
import org.humanbrainproject.knowledgegraph.indexing.entity.IndexingMessage;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusInstanceReference;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusSchemaReference;
import org.humanbrainproject.knowledgegraph.query.entity.JsonDocument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class NexusReleasingController {

    @Autowired
    NexusClient nexusClient;

    @Autowired
    InstanceController instanceController;

    @Autowired
    SchemaController schemaController;

    @Autowired
    NexusConfiguration configuration;

    @Autowired
    JsonTransformer jsonTransformer;

    @Autowired
    NexusToArangoIndexingProvider nexusToArangoIndexingProvider;

    public IndexingMessage release(NexusInstanceReference instanceReference, Integer revision, Credential credential) {
        JsonDocument payload = new JsonDocument();
        payload.addReference(HBPVocabulary.RELEASE_INSTANCE, configuration.getAbsoluteUrl(instanceReference));
        payload.put(HBPVocabulary.RELEASE_REVISION, revision);
        payload.addType(HBPVocabulary.RELEASE_TYPE);
        NexusSchemaReference releaseSchema = new NexusSchemaReference("releasing", "prov", "release", "v0.0.2");
        NexusInstanceReference instance = instanceController.createInstanceByIdentifier(releaseSchema, instanceReference.getFullId(false), payload, credential);
        return new IndexingMessage(instance, jsonTransformer.getMapAsJson(payload), null, null);
    }

    public Set<NexusInstanceReference> unrelease(NexusInstanceReference instanceReference, Credential credential) {
        //Find release instance
        Set<NexusInstanceReference> releases = nexusToArangoIndexingProvider.findInstancesWithLinkTo(HBPVocabulary.RELEASE_INSTANCE, instanceReference, credential);
        for (NexusInstanceReference nexusInstanceReference : releases) {
            instanceController.deprecateInstanceByNexusId(nexusInstanceReference, credential);
        }
        return releases;
    }

}
