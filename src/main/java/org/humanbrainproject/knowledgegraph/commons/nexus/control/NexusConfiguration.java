package org.humanbrainproject.knowledgegraph.commons.nexus.control;

import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusInstanceReference;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusRelativeUrl;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusSchemaReference;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class NexusConfiguration {

    public enum ResourceType {
        DATA("data"), SCHEMA("schemas"), ORGANIZATION("organizations"), DOMAIN("domains");

        private final String urlDeclaration;

        ResourceType(String urlDeclaration) {
            this.urlDeclaration = urlDeclaration;
        }

    }


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


    public String getNexusBase(ResourceType resourceType){
        return String.format("%s/v0/%s", nexusBase, resourceType.urlDeclaration);

    }

    public String getAbsoluteUrl(NexusRelativeUrl relativeUrl) {
        return String.format("%s/v0/%s%s", nexusEndpoint, relativeUrl.getResourceType().urlDeclaration, relativeUrl.getUrl() != null ? String.format("/%s", relativeUrl.getUrl()) : "");
    }

    public String getAbsoluteUrl(NexusSchemaReference schema) {
        return getAbsoluteUrl(schema.getRelativeUrl());
    }

    public String getAbsoluteUrl(NexusInstanceReference instanceReference) {
        return getAbsoluteUrl(instanceReference.getRelativeUrl());
    }

    public String getExternalAbsoluteUrl(NexusInstanceReference instanceReference) {
        return String.format("%s%s", getNexusBase(instanceReference.getRelativeUrl().getResourceType()), instanceReference.getRelativeUrl().getUrl() != null ? String.format("/%s", instanceReference.getRelativeUrl().getUrl()) : "");
    }
}
