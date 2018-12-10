package org.humanbrainproject.knowledgegraph.releasing.boundary;

import org.humanbrainproject.knowledgegraph.commons.authorization.entity.Credential;
import org.humanbrainproject.knowledgegraph.commons.authorization.entity.InternalMasterKey;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.ArangoDatabaseFactory;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.ArangoRepository;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoDocumentReference;
import org.humanbrainproject.knowledgegraph.commons.vocabulary.ArangoVocabulary;
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

    @Autowired
    ArangoRepository arangoRepository;

    @Autowired
    ArangoDatabaseFactory databaseFactory;


    public void release(NexusInstanceReference instanceReference, Credential accessToken) {
        NexusInstanceReference nexusInstanceFromInferredArangoEntry = releaseControl.findNexusInstanceFromInferredArangoEntry(ArangoDocumentReference.fromNexusInstance(instanceReference), accessToken);
        nexusReleasingController.release(nexusInstanceFromInferredArangoEntry, nexusInstanceFromInferredArangoEntry.getRevision(), accessToken);
    }

    public NexusInstanceReference unrelease(NexusInstanceReference instanceReference, Credential accessToken) {
        //We need the original id because the releasing mechanism needs to point to the real instance to ensure the right revision. We can do that by pointing to the nexus relative url of the inferred instance.
        Map document = arangoRepository.getDocument(ArangoDocumentReference.fromNexusInstance(instanceReference), databaseFactory.getInferredDB(), new InternalMasterKey());
        if (document != null) {
            Object relativeUrl = document.get(ArangoVocabulary.NEXUS_RELATIVE_URL);
            if (relativeUrl != null) {
                NexusInstanceReference fromUrl = NexusInstanceReference.createFromUrl((String) relativeUrl);
                nexusReleasingController.unrelease(fromUrl, accessToken);
                return fromUrl;
            }
        }
        return null;
    }

    public ReleaseStatusResponse getReleaseStatus(NexusInstanceReference instanceReference, boolean withChildren, Credential accessToken) {
        return releaseControl.getReleaseStatus(instanceReference, withChildren, accessToken);
    }

    public Map<String, Object> getReleaseGraph(NexusInstanceReference instanceReference, Credential accessToken) {
        return releaseControl.getReleaseGraph(instanceReference, Optional.empty(), accessToken);
    }

}
