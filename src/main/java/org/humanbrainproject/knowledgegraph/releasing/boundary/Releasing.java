package org.humanbrainproject.knowledgegraph.releasing.boundary;

import org.humanbrainproject.knowledgegraph.annotations.ToBeTested;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.builders.TreeScope;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusInstanceReference;
import org.humanbrainproject.knowledgegraph.releasing.control.ReleaseControl;
import org.humanbrainproject.knowledgegraph.releasing.entity.ReleaseStatusResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@ToBeTested(integrationTestRequired = true, systemTestRequired = true)
public class Releasing {

    @Autowired
    ReleaseControl releaseControl;

    public void release(NexusInstanceReference instanceReference) {
        releaseControl.release(instanceReference);
    }

    public NexusInstanceReference unrelease(NexusInstanceReference instanceReference) {
        return releaseControl.unrelease(instanceReference);
    }

    public ReleaseStatusResponse getReleaseStatus(NexusInstanceReference instanceReference, TreeScope scope) {
        return releaseControl.getReleaseStatus(instanceReference, scope);
    }

    public Map<String, Object> getReleaseGraph(NexusInstanceReference instanceReference) {
        return releaseControl.getReleaseGraph(instanceReference, TreeScope.ALL);
    }

}
