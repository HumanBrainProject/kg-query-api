package org.humanbrainproject.knowledgegraph.releasing.boundary;

import org.humanbrainproject.knowledgegraph.commons.authorization.entity.Credential;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoDocumentReference;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusInstanceReference;
import org.humanbrainproject.knowledgegraph.instances.control.NexusReleasingController;
import org.humanbrainproject.knowledgegraph.releasing.control.ReleaseControl;
import org.humanbrainproject.knowledgegraph.releasing.entity.ReleaseStatusResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Component
public class Releasing {

    @Autowired
    ReleaseControl releaseControl;

    @Autowired
    NexusReleasingController nexusReleasingController;

    public void release(NexusInstanceReference instanceReference, Credential accessToken){
        NexusInstanceReference nexusInstanceFromInferredArangoEntry = releaseControl.findNexusInstanceFromInferredArangoEntry(ArangoDocumentReference.fromNexusInstance(instanceReference), accessToken);
        nexusReleasingController.release(nexusInstanceFromInferredArangoEntry, nexusInstanceFromInferredArangoEntry.getRevision(), accessToken);
    }

    public void unrelease(NexusInstanceReference instanceReference, Credential accessToken){
        nexusReleasingController.unrelease(instanceReference, accessToken);
    }

    public ReleaseStatusResponse getReleaseStatus(NexusInstanceReference instanceReference, Credential accessToken){
        return releaseControl.getReleaseStatus(instanceReference, accessToken);
    }

    public Map<String, Object> getReleaseGraph(NexusInstanceReference instanceReference, Credential accessToken){
        return releaseControl.getReleaseGraph(instanceReference, Optional.empty(), accessToken);
    }

}
