package org.humanbrainproject.knowledgegraph.commons.nexus.control;


import org.humanbrainproject.knowledgegraph.commons.authorization.entity.OidcAccessToken;
import org.humanbrainproject.knowledgegraph.commons.jsonld.control.JsonTransformer;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusRelativeUrl;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusSchemaReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * This class is the interface for interacting with nexus on behalf of the user. It should never use the
 * system credentials for accessing Nexus.
 */
@Component
public class NexusClient {

    @Autowired
    NexusConfiguration configuration;

    @Autowired
    JsonTransformer jsonTransformer;


    public Set<String> getAllOrganizations(OidcAccessToken authorizationToken) {
        List<Map> list = list(new NexusRelativeUrl(NexusConfiguration.ResourceType.ORGANIZATION, "&size=100"), authorizationToken, true);
        return list.stream().map(org -> org.get("resultId").toString()).collect(Collectors.toSet());
    }


    private HttpHeaders createHeaders(OidcAccessToken oidcAccessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.parseMediaType("application/ld+json")));
        if (oidcAccessToken != null) {
            headers.set(HttpHeaders.AUTHORIZATION, oidcAccessToken.getBearerToken());
        }
        return headers;
    }

    public Map put(NexusRelativeUrl url, Integer revision, Map payload, OidcAccessToken oidcAccessToken) {
        ResponseEntity<Map> result = new RestTemplate().exchange(String.format("%s%s", configuration.getAbsoluteUrl(url), revision != null ? String.format("%srev=%d", !url.getUrl().contains("?") ? "?" : "&", revision) : ""), HttpMethod.PUT, new HttpEntity<>(payload, createHeaders(oidcAccessToken)), Map.class);
        if (result.getStatusCode().is2xxSuccessful() && result.getBody() != null) {
            return result.getBody();
        }
        return null;
    }

    public Map delete(NexusRelativeUrl url, Integer revision, OidcAccessToken oidcAccessToken) {
        ResponseEntity<Map> result = new RestTemplate().exchange(String.format("%s%s", configuration.getAbsoluteUrl(url), revision != null ? String.format("%srev=%d", !url.getUrl().contains("?") ? "?" : "&", revision) : ""), HttpMethod.DELETE, new HttpEntity<>(createHeaders(oidcAccessToken)), Map.class);
        if (result.getStatusCode().is2xxSuccessful() && result.getBody() != null) {
            return result.getBody();
        }
        return null;
    }

    public Map post(NexusRelativeUrl url, Integer revision, Map payload, OidcAccessToken oidcAccessToken) {
        ResponseEntity<Map> result = new RestTemplate().exchange(String.format("%s%s", configuration.getAbsoluteUrl(url), revision != null ? String.format("%srev=%d", !url.getUrl().contains("?") ? "?" : "&", revision) : ""), HttpMethod.POST, new HttpEntity<>(payload, createHeaders(oidcAccessToken)), Map.class);
        if (result.getStatusCode().is2xxSuccessful() && result.getBody() != null) {
            return result.getBody();
        }
        return null;
    }

    public Map patch(NexusRelativeUrl url, Integer revision, Map payload, OidcAccessToken oidcAccessToken) {
        RestTemplate template = new RestTemplate();
        template.setRequestFactory(new HttpComponentsClientHttpRequestFactory());
        ResponseEntity<Map> result = template.exchange(String.format("%s%s", configuration.getAbsoluteUrl(url), revision != null ? String.format("%srev=%d", !url.getUrl().contains("?") ? "?" : "&", revision) : ""), HttpMethod.PATCH, new HttpEntity<>(payload, createHeaders(oidcAccessToken)), Map.class);
        if (result.getStatusCode().is2xxSuccessful() && result.getBody() != null) {
            return result.getBody();
        }
        return null;
    }


    public List<Map> find(NexusSchemaReference nexusSchemaReference, String fieldName, String fieldValue, OidcAccessToken oidcAccessToken) {
        NexusRelativeUrl relativeUrl = new NexusRelativeUrl(NexusConfiguration.ResourceType.DATA, nexusSchemaReference.getRelativeUrl().getUrl());
        relativeUrl.addQueryParameter("fields", "all");
        relativeUrl.addQueryParameter("filter", String.format("{\"op\":\"eq\",\"path\":\"%s\",\"value\":\"%s\"}", fieldName, fieldValue));
        String url = configuration.getAbsoluteUrl(relativeUrl);
        ResponseEntity<Map> result = new RestTemplate().exchange(url, HttpMethod.GET, new HttpEntity<>(createHeaders(oidcAccessToken)), Map.class);
        if (result.getStatusCode().is2xxSuccessful() && result.getBody() != null && result.getBody().containsKey("results") && result.getBody().get("results") instanceof List) {
            return (List<Map>) result.getBody().get("results");
        }
        return Collections.emptyList();
    }


    public Map get(NexusRelativeUrl url, OidcAccessToken oidcAccessToken) {
        return get(url, oidcAccessToken, Map.class);
    }


    public <T> T get(NexusRelativeUrl url, OidcAccessToken oidcAccessToken, Class<T> resultClass) {
        try {
            ResponseEntity<T> result = new RestTemplate().exchange(configuration.getAbsoluteUrl(url), HttpMethod.GET, new HttpEntity<>(createHeaders(oidcAccessToken)), resultClass);
            if (result.getStatusCode().is2xxSuccessful() && result.getBody() != null) {
                return result.getBody();
            }
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return null;
            }
            throw exception;
        }
        return null;
    }

    public List<Map> list(NexusRelativeUrl relativeUrl, OidcAccessToken oidcAccessToken, boolean followPages) {
        ResponseEntity<Map> result = new RestTemplate().exchange(configuration.getAbsoluteUrl(relativeUrl), HttpMethod.GET, new HttpEntity<>(createHeaders(oidcAccessToken)), Map.class);
        if (result.getStatusCode().is2xxSuccessful() && result.getBody() != null && result.getBody().containsKey("results") && result.getBody().get("results") instanceof List) {
            List<Map> results = (List<Map>) result.getBody().get("results");
            if (followPages) {
                Object links = result.getBody().get("links");
                if (links instanceof Map) {
                    Object next = ((Map) links).get("next");
                    if (next instanceof String) {
                        List<Map> nextPage = list(new NexusRelativeUrl(relativeUrl.getResourceType(), (String) next), oidcAccessToken, true);
                        results.addAll(nextPage);
                    }
                }
            }
            return results;
        }
        return Collections.emptyList();
    }


}
