package org.humanbrainproject.knowledgegraph.releasing.boundary;

import org.humanbrainproject.knowledgegraph.commons.authorization.entity.OidcAccessToken;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoDocumentReference;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusInstanceReference;
import org.humanbrainproject.knowledgegraph.nexusExt.control.NexusReleasingController;
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

    public void release(NexusInstanceReference instanceReference, OidcAccessToken accessToken){
        NexusInstanceReference nexusInstanceFromInferredArangoEntry = releaseControl.findNexusInstanceFromInferredArangoEntry(ArangoDocumentReference.fromNexusInstance(instanceReference));
        nexusReleasingController.release(nexusInstanceFromInferredArangoEntry, nexusInstanceFromInferredArangoEntry.getRevision(), accessToken);
    }

    public void unrelease(NexusInstanceReference instanceReference, OidcAccessToken accessToken){
        nexusReleasingController.unrelease(instanceReference, accessToken);
    }

    public ReleaseStatusResponse getReleaseStatus(NexusInstanceReference instanceReference){
        return releaseControl.getReleaseStatus(instanceReference);
    }

    public Map<String, Object> getReleaseGraph(NexusInstanceReference instanceReference){
        return releaseControl.getReleaseGraph(instanceReference, Optional.empty());
    }

}
