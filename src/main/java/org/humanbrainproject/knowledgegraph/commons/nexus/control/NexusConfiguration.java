package org.humanbrainproject.knowledgegraph.commons.nexus.control;

import org.humanbrainproject.knowledgegraph.annotations.Tested;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusInstanceReference;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusRelativeUrl;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusSchemaReference;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Tested
public class NexusConfiguration {

    public enum ResourceType {
        RESOURCES("resources"), SCHEMA("schemas"), ORGANIZATION("orgs"), PROJECTS("projects"), IAM("acls"), RESOLVERS("resolvers");

        private final String urlDeclaration;

        ResourceType(String urlDeclaration) {
            this.urlDeclaration = urlDeclaration;
        }

    }

    @Value("${org.humanbrainproject.knowledgegraph.nexus.base}")
    String nexusBase;

    @Value("${org.humanbrainproject.knowledgegraph.nexus.endpoint}")
    String nexusEndpoint;

    @Value("${org.humanbrainproject.knowledgegraph.iam.endpoint}")
    String iamEndpoint;

    /*
     * @return the resolvable nexus-endpoint URL. This is the URL where the API endpoint can be accessed. Please note, that this can but not necessarily has to be the same as nexusBase since reverse proxies and network topologies can change this
     */
    public String getNexusEndpoint() {
        return nexusEndpoint;
    }

    public String getUserInfoEndpoint(){
        return String.format("%s/v0/oauth2/userinfo", iamEndpoint);
    }


    /**
     * @return the nexus-base URL. This is the URL which is used e.g. to prefix the IDs of nexus instances.
     */
    public String getNexusBase() {
        return nexusBase;
    }

    public String getNexusBase(ResourceType resourceType){
        return String.format("%s/%s", getNexusBase(), resourceType.urlDeclaration);
    }

    public String getNexusEndpoint(ResourceType resourceType){
        return String.format("%s/%s", getNexusEndpoint(), resourceType.urlDeclaration);
    }

    public String getIamEndpoint(ResourceType resourceType){
        return String.format("%s/%s", iamEndpoint, resourceType.urlDeclaration);
    }

    public String getNexusEndpoint(NexusRelativeUrl relativeUrl) {
        String endpoint;

        if(relativeUrl.getResourceType()==ResourceType.IAM){
            endpoint = getIamEndpoint(relativeUrl.getResourceType());
        }
        else{
            endpoint = getNexusEndpoint(relativeUrl.getResourceType());
        }
        return String.format("%s%s", endpoint, relativeUrl.getUrl() != null ? String.format("/%s", relativeUrl.getUrl()) : "");
    }

    public String getAbsoluteUrl(NexusSchemaReference schemaReference){
        return String.format("%s%s", getNexusEndpoint(schemaReference.getRelativeUrl().getResourceType()), schemaReference.getRelativeUrl().getUrl() != null ? String.format("/%s", schemaReference.getRelativeUrl().getUrl()) : "");
    }

    public String getAbsoluteUrl(NexusInstanceReference instanceReference) {
        return String.format("%s%s", getNexusEndpoint(instanceReference.getRelativeUrl().getResourceType()), instanceReference.getRelativeUrl().getUrl() != null ? String.format("/%s", instanceReference.getRelativeUrl().getUrl()) : "");
    }

}
