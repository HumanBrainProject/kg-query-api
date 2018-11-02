package org.humanbrainproject.knowledgegraph.releasing.boundary;

import org.humanbrainproject.knowledgegraph.commons.authorization.entity.OidcAccessToken;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoDocumentReference;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusInstanceReference;
import org.humanbrainproject.knowledgegraph.nexusExt.control.NexusReleasingController;
import org.humanbrainproject.knowledgegraph.releasing.control.ReleaseControl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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

}
