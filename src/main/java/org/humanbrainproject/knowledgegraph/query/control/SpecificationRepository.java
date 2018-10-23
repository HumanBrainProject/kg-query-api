package org.humanbrainproject.knowledgegraph.query.control;

import org.humanbrainproject.knowledgegraph.nexus.control.NexusConfiguration;
import org.humanbrainproject.knowledgegraph.query.entity.Template;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.Map;

@Component
public class SpecificationRepository {

    @Autowired
    NexusConfiguration configuration;

    private final static String ORGANIZATION = "kgquery";
    private final static String DOMAIN="core";
    private final static String SCHEMA="specification";
    private final static String SCHEMA_VERSION="v0.0.1";

    private String getNexusBaseUrl(){
        return String.format("%s/v0/", configuration.getNexusBase());
    }

    private void createOrganizationIfNotExists(String name, String authorizationToken){
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = createHeaders(authorizationToken);
        String url = String.format("%s/organizations/%s", getNexusBaseUrl(), name);
        try {
            restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<String>(headers), Map.class);
        }
        catch(HttpClientErrorException e){
            if(HttpStatus.NOT_FOUND == e.getStatusCode()){
                restTemplate.exchange(url, HttpMethod.PUT, new HttpEntity<String>(String.format("{\"http://schema.org/name\": \"%s\"}", name), headers), Map.class);
            }
        }
    }

    private void createDomainIfNotExists(String organization, String name,  String authorizationToken){
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = createHeaders(authorizationToken);
        String url = String.format("%s/organizations/%s/domains/%s", getNexusBaseUrl(), organization, name);
        try {
            restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<String>(headers), Map.class);
        }
        catch(HttpClientErrorException e){
            if(HttpStatus.NOT_FOUND == e.getStatusCode()){
                restTemplate.exchange(url, HttpMethod.PUT, new HttpEntity<String>(String.format("{\"http://schema.org/name\": \"%s\"}", name), headers), Map.class);
            }
        }
    }


    private void createSchemaIfNotExists(String organization, String domain, String schema, String schema_version, String authorizationToken){
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = createHeaders(authorizationToken);
        String url = String.format("%s/schemas/%s/%s/%s/%s", getNexusBaseUrl(), organization, domain, schema, schema_version);
        try {
            restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<String>(headers), Map.class);
        }
        catch(HttpClientErrorException e){
            if(HttpStatus.NOT_FOUND == e.getStatusCode()){
                restTemplate.exchange(url, HttpMethod.PUT, new HttpEntity<String>(String.format("{\"http://schema.org/name\": \"%s\"}", schema), headers), Map.class);
            }
        }
    }

//    private String createSchema(){
//
//    }


    private HttpHeaders createHeaders(String authorizationToken){
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.parseMediaType("application/ld+json")));
        if (authorizationToken != null) {
            headers.set(HttpHeaders.AUTHORIZATION, authorizationToken);
        }
        return headers;
    }


    public void saveTemplate(Template template){



    }


//    public void saveSpecification(String jsonPayload, String authorizationToken){
//        createOrganizationIfNotExists(ORGANIZATION, authorizationToken);
//        createDomainIfNotExists(ORGANIZATION, DOMAIN, authorizationToken);
//
//
//        RestTemplate restTemplate = new RestTemplate();
//        HttpHeaders headers = new HttpHeaders();
//        headers.setContentType(MediaType.APPLICATION_JSON);
//        headers.setAccept(Collections.singletonList(MediaType.parseMediaType("application/ld+json")));
//        if (authorizationToken != null) {
//            headers.set(HttpHeaders.AUTHORIZATION, authorizationToken);
//        }
//        HttpEntity<String> entity = new HttpEntity<>(headers);
//        try {
//            ResponseEntity<Map> result = restTemplate.exchange(String.format("%s?size=100", getNexusOrganizationsUrl()), HttpMethod.GET, entity, Map.class);
//            if(result.getBody().containsKey("results")) {
//                List<Map> results = (List<Map>)result.getBody().get("results");
//                if(results!=null){
//                    Set<String> organizations = results.stream().map(r -> r.get("resultId").toString().replaceAll(getNexusOrganizationsUrl(), "")).collect(Collectors.toSet());
//                    tokenToOrganizations.put(authorizationToken, organizations);
//                    return organizations;
//                }
//            }
//        } catch (HttpClientErrorException e) {
//            if (HttpStatus.FORBIDDEN == e.getStatusCode()) {
//                return Collections.singleton(PUBLIC_GROUP_NAME);
//            } else {
//                throw e;
//            }
//        }
//    }


}
