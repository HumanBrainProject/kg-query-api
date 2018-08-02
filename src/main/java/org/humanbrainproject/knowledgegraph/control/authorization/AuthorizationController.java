package org.humanbrainproject.knowledgegraph.control.authorization;

import org.apache.commons.collections.map.LRUMap;
import org.humanbrainproject.knowledgegraph.control.Configuration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class AuthorizationController {

    @Autowired
    Configuration configuration;

    private final Map<String, Set<String>> tokenToOrganizations = new LRUMap();


    private final static String PUBLIC_GROUP_NAME="public";

    private String getNexusOrganizationsUrl(){
        return String.format("%s/v0/organizations/", configuration.getNexusBase());
    }

    public Set<String> getOrganizations(String authorizationToken) {
        if(tokenToOrganizations.containsKey(authorizationToken)){
            return (Set<String>)tokenToOrganizations.get(authorizationToken);
        }
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.parseMediaType("application/ld+json")));
        if (authorizationToken != null) {
            headers.set(HttpHeaders.AUTHORIZATION, authorizationToken);
        }
        HttpEntity<String> entity = new HttpEntity<>(headers);
        try {
            ResponseEntity<Map> result = restTemplate.exchange(String.format("%s?size=100", getNexusOrganizationsUrl()), HttpMethod.GET, entity, Map.class);
            if(result.getBody().containsKey("results")) {
                List<Map> results = (List<Map>)result.getBody().get("results");
                if(results!=null){
                    Set<String> organizations = results.stream().map(r -> r.get("resultId").toString().replaceAll(getNexusOrganizationsUrl(), "")).collect(Collectors.toSet());
                    tokenToOrganizations.put(authorizationToken, organizations);
                    return organizations;
                }
            }
        } catch (HttpClientErrorException e) {
            if (HttpStatus.FORBIDDEN == e.getStatusCode()) {
                return Collections.singleton(PUBLIC_GROUP_NAME);
            } else {
                throw e;
            }
        }
        return null;
    }


}
