package org.humanbrainproject.knowledgegraph.authorization.control;

import org.apache.commons.collections4.map.LRUMap;
import org.humanbrainproject.knowledgegraph.authorization.entity.AccessRight;
import org.humanbrainproject.knowledgegraph.authorization.entity.OidcAccessToken;
import org.humanbrainproject.knowledgegraph.nexus.control.NexusConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class AuthorizationController {

    @Autowired
    NexusConfiguration configuration;

    private final LRUMap<OidcAccessToken, Set<AccessRight>> tokenToAccessRights = new LRUMap<>();

    private final static String PUBLIC_GROUP_NAME = "public";

    private String getNexusOrganizationsUrl() {
        return String.format("%s/v0/organizations/", configuration.getNexusBase());
    }

    public Set<AccessRight> getAccessRights(OidcAccessToken authorizationToken) {
        if (tokenToAccessRights.containsKey(authorizationToken)) {
            return tokenToAccessRights.get(authorizationToken);
        }
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.parseMediaType("application/ld+json")));
        if (authorizationToken != null) {
            headers.set(HttpHeaders.AUTHORIZATION, authorizationToken.getBearerToken());
        }
        HttpEntity<String> entity = new HttpEntity<>(headers);
        ResponseEntity<Map> result = restTemplate.exchange(String.format("%s?size=100", getNexusOrganizationsUrl()), HttpMethod.GET, entity, Map.class);
        if (result.getBody()!=null && result.getBody().containsKey("results")) {
            Object results = result.getBody().get("results");
            if(results instanceof List){
                Set<AccessRight> accessRights = ((List<?>)results).stream().map(
                        r -> r instanceof Map ? new AccessRight(((Map)r).get("resultId").toString().replaceAll(getNexusOrganizationsUrl(), ""), AccessRight.Permission.READ)
                                : null).filter(Objects::nonNull).collect(Collectors.toSet());
                tokenToAccessRights.put(authorizationToken, accessRights);
                return accessRights;
            }
        }
        return null;
    }


}
