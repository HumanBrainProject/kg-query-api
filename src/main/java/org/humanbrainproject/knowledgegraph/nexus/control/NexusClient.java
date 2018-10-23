package org.humanbrainproject.knowledgegraph.nexus.control;


import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusInstanceReference;
import org.humanbrainproject.knowledgegraph.jsonld.control.JsonTransformer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class NexusClient {

    @Autowired
    NexusConfiguration configuration;

    @Autowired
    SystemOidcHeaderInterceptor systemOidc;


    @Autowired
    JsonTransformer jsonTransformer;


    public final String getPayloadWithTechnicalUser(NexusInstanceReference nexusInstanceReference) {
        RestTemplate restTemplate = new RestTemplateBuilder().interceptors(systemOidc).build();
        return restTemplate.getForObject(configuration.getAbsoluteUrl(nexusInstanceReference), String.class);
    }

    public void updateInstanceByRelativePath(String path, String payload, String authentication){
        OidcHeaderInterceptor oidcAuth = new OidcHeaderInterceptor(authentication);
        RestTemplate restTemplate = new RestTemplateBuilder().interceptors(oidcAuth).build();
        restTemplate.put(String.format("%s/v0/data/%s", configuration.getNexusEndpoint(), path), payload, String.class);
    }

    public void createInstance(String entityName, String payload, String authenticationToken){
        OidcHeaderInterceptor oidcAuth = new OidcHeaderInterceptor(authenticationToken);
        RestTemplate restTemplate = new RestTemplateBuilder().interceptors(oidcAuth).build();
        String result = restTemplate.postForObject(String.format("%s/v0/data/%s", configuration.getNexusEndpoint(), entityName), payload, String.class);
        System.out.println(result);
    }


    public void deprecateInstance(String entityName, String id, Integer rev, String authenticationToken){
        String path = String.format("%s/%s?rev=%d", entityName, id, rev);
        deprecateInstanceByRelativePath(path, authenticationToken);
    }

    public void deprecateInstanceByRelativePath(String path, String authenticationToken){
        OidcHeaderInterceptor oidcAuth = new OidcHeaderInterceptor(authenticationToken);
        RestTemplate restTemplate = new RestTemplateBuilder().interceptors(oidcAuth).build();
        restTemplate.delete(String.format("%s/v0/data/%s", configuration.getNexusEndpoint(), path));
    }

}
