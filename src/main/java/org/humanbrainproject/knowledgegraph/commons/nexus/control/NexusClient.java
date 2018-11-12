package org.humanbrainproject.knowledgegraph.commons.nexus.control;


import org.humanbrainproject.knowledgegraph.commons.authorization.control.OidcHeaderInterceptor;
import org.humanbrainproject.knowledgegraph.commons.authorization.entity.OidcAccessToken;
import org.humanbrainproject.knowledgegraph.commons.jsonld.control.JsonTransformer;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusRelativeUrl;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusSchemaReference;
import org.humanbrainproject.knowledgegraph.query.entity.JsonDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequestInterceptor;
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

    protected Logger logger = LoggerFactory.getLogger(NexusClient.class);

    public Set<String> getAllOrganizations(OidcAccessToken authorizationToken) {
        List<JsonDocument> list = list(new NexusRelativeUrl(NexusConfiguration.ResourceType.ORGANIZATION, "&size=100"), authorizationToken, true);
        return list.stream().map(org -> org.get("resultId").toString()).collect(Collectors.toSet());
    }

    public Set<NexusSchemaReference> getAllSchemas(ClientHttpRequestInterceptor oidc) {
        List<JsonDocument> schemas = list(new NexusRelativeUrl(NexusConfiguration.ResourceType.SCHEMA, ""), oidc, true);
        return schemas.stream().map(schema -> NexusSchemaReference.createFromUrl(schema.get("resultId").toString())).collect(Collectors.toSet());
    }

    public JsonDocument put(NexusRelativeUrl url, Integer revision, Map payload, OidcAccessToken oidc) {
        return put(url, revision, payload, new OidcHeaderInterceptor(oidc));
    }

    public JsonDocument put(NexusRelativeUrl url, Integer revision, Map payload, ClientHttpRequestInterceptor oidc) {
        RestTemplate template = new RestTemplate();
        template.setInterceptors(Collections.singletonList(oidc));
        ResponseEntity<Map> result = template.exchange(String.format("%s%s", configuration.getEndpoint(url), revision != null ? String.format("%srev=%d", !url.getUrl().contains("?") ? "?" : "&", revision) : ""), HttpMethod.PUT, new HttpEntity<>(payload), Map.class);
        if (result.getStatusCode().is2xxSuccessful() && result.getBody() != null) {
            return new JsonDocument(result.getBody());
        }
        return null;
    }


    public boolean delete(NexusRelativeUrl url, Integer revision, OidcAccessToken oidcAccessToken) {
        return delete(url, revision, new OidcHeaderInterceptor(oidcAccessToken));
    }

    public boolean delete(NexusRelativeUrl url, Integer revision, ClientHttpRequestInterceptor oidc) {
        try {
            RestTemplate template = new RestTemplate();
            template.setInterceptors(Collections.singletonList(oidc));
            template.delete(String.format("%s%s", configuration.getEndpoint(url), revision != null ? String.format("%srev=%d", !url.getUrl().contains("?") ? "?" : "&", revision) : ""));
            return true;
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.CONFLICT) {
                logger.info("Was not able to remove the instance {} due to a conflict. It seems as it is already deprecated", url);
                return false;
            }
            else{
                logger.error("Was not able to delete the instance", e);
            }

        }
        return false;
    }

    public JsonDocument post(NexusRelativeUrl url, Integer revision, Map payload, OidcAccessToken oidcAccessToken) {
        return post(url, revision, payload, new OidcHeaderInterceptor(oidcAccessToken));
    }

    public JsonDocument post(NexusRelativeUrl url, Integer revision, Map payload, ClientHttpRequestInterceptor oidc) {
        try {
            RestTemplate template = new RestTemplate();
            template.setInterceptors(Collections.singletonList(oidc));
            ResponseEntity<Map> result = template.exchange(String.format("%s%s", configuration.getEndpoint(url), revision != null ? String.format("%srev=%d", !url.getUrl().contains("?") ? "?" : "&", revision) : ""), HttpMethod.POST, new HttpEntity<>(payload), Map.class);
            if (result.getStatusCode().is2xxSuccessful() && result.getBody() != null) {
                return new JsonDocument(result.getBody());
            }
        } catch (HttpClientErrorException e) {
            logger.error("Was not able to create instance in nexus", e.getResponseBodyAsString());
            throw e;
        }
        return null;
    }


    public JsonDocument patch(NexusRelativeUrl url, Integer revision, Map payload, OidcAccessToken oidcAccessToken) {
        return patch(url, revision, payload, new OidcHeaderInterceptor(oidcAccessToken));
    }

    JsonDocument patch(NexusRelativeUrl url, Integer revision, Map payload, ClientHttpRequestInterceptor oidc) {
        RestTemplate template = new RestTemplate();
        template.setInterceptors(Collections.singletonList(oidc));
        template.setRequestFactory(new HttpComponentsClientHttpRequestFactory());
        ResponseEntity<Map> result = template.exchange(String.format("%s%s", configuration.getEndpoint(url), revision != null ? String.format("%srev=%d", !url.getUrl().contains("?") ? "?" : "&", revision) : ""), HttpMethod.PATCH, new HttpEntity<>(payload), Map.class);
        if (result.getStatusCode().is2xxSuccessful() && result.getBody() != null) {
            return new JsonDocument(result.getBody());
        }
        return null;
    }

    public List<JsonDocument> find(NexusSchemaReference nexusSchemaReference, String fieldName, String fieldValue, OidcAccessToken oidcAccessToken) {
        return find(nexusSchemaReference, fieldName, fieldValue, new OidcHeaderInterceptor(oidcAccessToken));
    }

    List<JsonDocument> find(NexusSchemaReference nexusSchemaReference, String fieldName, String fieldValue, ClientHttpRequestInterceptor oidc) {
        NexusRelativeUrl relativeUrl = new NexusRelativeUrl(NexusConfiguration.ResourceType.DATA, nexusSchemaReference.getRelativeUrl().getUrl());
        relativeUrl.addQueryParameter("fields", "all");
        relativeUrl.addQueryParameter("deprecated", "false");
        relativeUrl.addQueryParameter("filter", String.format("{\"op\":\"eq\",\"path\":\"%s\",\"value\":\"%s\"}", fieldName, fieldValue));
        String url = configuration.getEndpoint(relativeUrl);
        RestTemplate template = new RestTemplate();
        template.setInterceptors(Collections.singletonList(oidc));
        ResponseEntity<Map> result = template.getForEntity(url, Map.class);
        if (result.getStatusCode().is2xxSuccessful() && result.getBody() != null && result.getBody().containsKey("results") && result.getBody().get("results") instanceof List) {
            return (List<JsonDocument>) ((List) result.getBody().get("results")).stream().filter(r -> r instanceof Map).map(r -> new JsonDocument((Map) r)).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }


    public JsonDocument get(NexusRelativeUrl url, OidcAccessToken oidcAccessToken) {
        return get(url, new OidcHeaderInterceptor(oidcAccessToken));
    }

    JsonDocument get(NexusRelativeUrl url, ClientHttpRequestInterceptor oidc) {
        Map map = get(url, oidc, Map.class);
        return map != null ? new JsonDocument(map) : null;
    }

    public <T> T get(NexusRelativeUrl url, OidcAccessToken oidcAccessToken, Class<T> resultClass) {
        return get(url, new OidcHeaderInterceptor(oidcAccessToken), resultClass);
    }

    <T> T get(NexusRelativeUrl url, ClientHttpRequestInterceptor oidc, Class<T> resultClass) {
        try {
            RestTemplate template = new RestTemplate();
            template.setInterceptors(Collections.singletonList(oidc));
            ResponseEntity<T> result = template.getForEntity(configuration.getEndpoint(url), resultClass);
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


    public List<JsonDocument> list(NexusRelativeUrl relativeUrl, OidcAccessToken oidcAccessToken, boolean followPages) {
        return list(relativeUrl, new OidcHeaderInterceptor(oidcAccessToken), followPages);
    }

    public List<JsonDocument> list(NexusSchemaReference schemaReference, OidcAccessToken oidcAccessToken, boolean followPages){
        return list(schemaReference, new OidcHeaderInterceptor(oidcAccessToken), followPages);
    }

    List<JsonDocument> list(NexusSchemaReference schemaReference, ClientHttpRequestInterceptor oidc, boolean followPages){
        return list(new NexusRelativeUrl(NexusConfiguration.ResourceType.DATA, schemaReference.getRelativeUrl().getUrl()), oidc, followPages);
    }


    public List<NexusSchemaReference> listSchemasByOrganization(String organization, OidcAccessToken oidc, boolean followPages) {
        NexusRelativeUrl relativeUrl = new NexusRelativeUrl(NexusConfiguration.ResourceType.SCHEMA, organization);
        List<JsonDocument> list = list(relativeUrl, oidc, followPages);
        return list.stream().map(d -> NexusSchemaReference.createFromUrl((String) d.get("resultId"))).collect(Collectors.toList());
    }

    List<JsonDocument> list(NexusRelativeUrl relativeUrl, ClientHttpRequestInterceptor oidc, boolean followPages) {
        return list(configuration.getEndpoint(relativeUrl)+"?deprecated=false", oidc, followPages);
    }

    private List<JsonDocument> list(String url, ClientHttpRequestInterceptor oidc, boolean followPages) {
        RestTemplate template = new RestTemplate();
        template.setInterceptors(Collections.singletonList(oidc));
        ResponseEntity<Map> result = template.getForEntity(url, Map.class);
        if (result.getStatusCode().is2xxSuccessful() && result.getBody() != null && result.getBody().containsKey("results") && result.getBody().get("results") instanceof List) {
            List<JsonDocument> results = (List<JsonDocument>) ((List) result.getBody().get("results")).stream().map(r -> new JsonDocument((Map) r)).collect(Collectors.toList());
            if (followPages) {
                Object links = result.getBody().get("links");
                if (links instanceof Map) {
                    Object next = ((Map) links).get("next");
                    if (next instanceof String) {
                        List<JsonDocument> nextPage = list((String)next, oidc, true);
                        results.addAll(nextPage);
                    }
                }
            }
            return results;
        }
        return Collections.emptyList();
    }




}
