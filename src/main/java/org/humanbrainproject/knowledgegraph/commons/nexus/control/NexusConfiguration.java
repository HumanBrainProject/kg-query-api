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
        return String.format("%s/v0/%s", getNexusBase(), resourceType.urlDeclaration);
    }

    public String getEndpoint(ResourceType resourceType){
        return String.format("%s/v0/%s", getNexusEndpoint(), resourceType.urlDeclaration);
    }

    public String getEndpoint(NexusRelativeUrl relativeUrl) {
        return String.format("%s%s", getEndpoint(relativeUrl.getResourceType()), relativeUrl.getUrl() != null ? String.format("/%s", relativeUrl.getUrl()) : "");
    }

    public String getAbsoluteUrl(NexusSchemaReference schemaReference){
        return String.format("%s%s", getNexusBase(schemaReference.getRelativeUrl().getResourceType()), schemaReference.getRelativeUrl().getUrl() != null ? String.format("/%s", schemaReference.getRelativeUrl().getUrl()) : "");
    }


    public String getAbsoluteUrl(NexusInstanceReference instanceReference) {
        return String.format("%s%s", getNexusBase(instanceReference.getRelativeUrl().getResourceType()), instanceReference.getRelativeUrl().getUrl() != null ? String.format("/%s", instanceReference.getRelativeUrl().getUrl()) : "");
    }

}
