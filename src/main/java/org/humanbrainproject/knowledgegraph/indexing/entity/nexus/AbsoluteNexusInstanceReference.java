package org.humanbrainproject.knowledgegraph.indexing.entity.nexus;

import org.humanbrainproject.knowledgegraph.commons.nexus.control.NexusConfiguration;

public class AbsoluteNexusInstanceReference {

    private final NexusInstanceReference instanceReference;
    private final String absoluteUrl;

    public AbsoluteNexusInstanceReference(NexusInstanceReference instanceReference, String absoluteUrl) {
        this.instanceReference = instanceReference;
        this.absoluteUrl = absoluteUrl;
    }

    public AbsoluteNexusInstanceReference(NexusInstanceReference instanceReference, NexusConfiguration nexusConfiguration) {
        this(instanceReference, nexusConfiguration.getAbsoluteUrl(instanceReference));
    }

    public String getAbsoluteUrl() {
        return absoluteUrl;
    }

    public NexusInstanceReference getInstanceReference() {
        return instanceReference;
    }
}
