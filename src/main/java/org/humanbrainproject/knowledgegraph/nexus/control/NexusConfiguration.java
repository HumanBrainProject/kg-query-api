package org.humanbrainproject.knowledgegraph.nexus.control;

import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusInstanceReference;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusSchemaReference;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class NexusConfiguration {

    private static final String DEFAULT_NAMESPACE = "http://schema.hbp.eu/internal#";

    @Value("${org.humanbrainproject.knowledgegraph.nexus.base}")
    String nexusBase;

    @Value("${org.humanbrainproject.knowledgegraph.nexus.endpoint}")
    String nexusEndpoint;


    public String getNexusEndpoint() {
        return nexusEndpoint;
    }

    public String getNexusBase() {
        return nexusBase;
    }

    public String getPermissionGroup(){
        return String.format("%s%s", DEFAULT_NAMESPACE, "permissionGroup");
    }

    private String getAbsoluteUrlForInstance(String relativeUrl){
        return String.format("%s/v0/data/%s", nexusEndpoint, relativeUrl);
    }

    public String getAbsoluteUrl(NexusSchemaReference schema) {
        return getAbsoluteUrlForInstance(schema.getRelativeUrl());
    }

    public String getAbsoluteUrl(NexusInstanceReference instanceReference){
        return getAbsoluteUrlForInstance(instanceReference.getRelativeUrl());
    }
}
