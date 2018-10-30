package org.humanbrainproject.knowledgegraph.nexusExt.control;

import com.github.jsonldjava.core.JsonLdConsts;
import org.humanbrainproject.knowledgegraph.commons.authorization.entity.OidcAccessToken;
import org.humanbrainproject.knowledgegraph.commons.jsonld.control.JsonTransformer;
import org.humanbrainproject.knowledgegraph.commons.nexus.control.NexusClient;
import org.humanbrainproject.knowledgegraph.commons.nexus.control.NexusConfiguration;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.entity.ReferenceType;
import org.humanbrainproject.knowledgegraph.indexing.control.nexusToArango.NexusToArangoIndexingProvider;
import org.humanbrainproject.knowledgegraph.indexing.entity.IndexingMessage;
import org.humanbrainproject.knowledgegraph.indexing.entity.InstanceReference;
import org.humanbrainproject.knowledgegraph.indexing.entity.knownSemantics.Release;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusInstanceReference;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusSchemaReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@Component
public class ReleaseController {

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

    public IndexingMessage release(NexusInstanceReference instanceReference, Integer revision, OidcAccessToken oidcAccessToken) throws IOException {
        Map<String, Object> payload = new LinkedHashMap<>();
        Map<String, Object> reference = new HashMap<>();
        reference.put(JsonLdConsts.ID, configuration.getAbsoluteUrl(instanceReference));
        payload.put(Release.RELEASE_REVISION_PROPERTYNAME, revision);
        payload.put(Release.RELEASE_INSTANCE_PROPERTYNAME, reference);
        payload.put(JsonLdConsts.TYPE, Release.RELEASE_TYPE);
        NexusSchemaReference releaseSchema = new NexusSchemaReference(instanceReference.getNexusSchema().getOrganization(), "prov", "release", "v0.0.1");
        NexusInstanceReference instance = instanceController.createInstanceByIdentifier(releaseSchema, instanceReference.getFullId(), payload, oidcAccessToken);
        return new IndexingMessage(instance, jsonTransformer.getMapAsJson(payload));
    }


    public void unrelease(NexusInstanceReference instanceReference, OidcAccessToken oidcAccessToken) {
        //Find release instance
        Set<? extends InstanceReference> releases = nexusToArangoIndexingProvider.findInstancesWithLinkTo(Release.RELEASE_INSTANCE_PROPERTYNAME, instanceReference, ReferenceType.INTERNAL);
        //Deprecate release instance
        for (InstanceReference release : releases) {
            if(release instanceof NexusInstanceReference){
                nexusClient.delete(((NexusInstanceReference)release).getRelativeUrl(), ((NexusInstanceReference)release).getRevision(), oidcAccessToken);
            }
        }
    }

}
