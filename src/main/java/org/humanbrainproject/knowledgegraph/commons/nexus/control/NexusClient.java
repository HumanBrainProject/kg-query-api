package org.humanbrainproject.knowledgegraph.commons.nexus.control;


import com.github.jsonldjava.core.JsonLdConsts;
import org.apache.commons.lang3.StringUtils;
import org.humanbrainproject.knowledgegraph.admin.entity.ACLEntry;
import org.humanbrainproject.knowledgegraph.annotations.ToBeTested;
import org.humanbrainproject.knowledgegraph.commons.authorization.control.AuthorizationController;
import org.humanbrainproject.knowledgegraph.commons.authorization.entity.Credential;
import org.humanbrainproject.knowledgegraph.commons.jsonld.control.JsonTransformer;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusInstanceReference;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusRelativeUrl;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusSchemaReference;
import org.humanbrainproject.knowledgegraph.query.entity.JsonDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * This class is the interface for interacting with nexus on behalf of the user. It should never use the
 * system credentials for accessing Nexus.
 */
@Component
@ToBeTested(systemTestRequired = true)
@Primary
public class NexusClient {

    @Autowired
    NexusConfiguration configuration;

    @Autowired
    JsonTransformer jsonTransformer;

    @Autowired
    AuthorizationController authorizationController;

    @Value("${org.humanbrainproject.knowledgegraph.iam.defaultRealm}")
    String defaultRealm;

    public String getDefaultRealm() {
        return defaultRealm;
    }

    protected Logger logger = LoggerFactory.getLogger(NexusClient.class);


    public void createOrganization(String name, Credential credential) {
        JsonDocument organization = new JsonDocument();
        organization.put("description", StringUtils.capitalize(name));
        NexusRelativeUrl org = new NexusRelativeUrl(NexusConfiguration.ResourceType.ORGANIZATION, name);
        try {
            put(org, null, organization, authorizationController.getInterceptor(credential));
        } catch (HttpClientErrorException e) {
            if ("OrganizationAlreadyExists".equals(getExceptionType(e))) {
                logger.info(String.format("Not creating organization %s because it already exists", name));
                return;
            }
            throw e;
        }
    }

    private String getExceptionType(HttpClientErrorException e) {
        Map map = jsonTransformer.parseToMap(e.getResponseBodyAsString());
        if (map != null) {
            return (String) map.get(JsonLdConsts.TYPE);
        }
        return null;
    }

    public void createProject(String organization, String name, Credential credential, String description, Map<String, String> prefixes) {
        JsonDocument project = new JsonDocument();
        project.put("description", String.format("%s space for %s - %s", StringUtils.capitalize(name), organization, description));
        project.put("base", String.format("https://schemas.hbp.eu/%s/", organization));
        project.put("vocab", String.format("https://schemas.hbp.eu/%s/", organization));
        NexusRelativeUrl proj = new NexusRelativeUrl(NexusConfiguration.ResourceType.PROJECTS, String.format("%s/%s", organization, name));
        JsonDocument fromDB = get(proj, credential);
        if (prefixes != null) {
            List<Map<String, String>> prefixMaps = new ArrayList<>();
            for (String key : prefixes.keySet()) {
                Map<String, String> mapping = new LinkedHashMap<>();
                mapping.put("prefix", key);
                mapping.put("namespace", prefixes.get(key));
                prefixMaps.add(mapping);
            }
            project.put("apiMappings", prefixMaps);
        } else if (fromDB != null) {
            project.put("apiMappings", fromDB.get("apiMappings"));
        }
        put(proj, fromDB == null ? null : (Integer) fromDB.get("_rev"), project, authorizationController.getInterceptor(credential));
    }


    public void setAccessRightsForProject(String organization, String project, List<ACLEntry> accessRights, Credential credential) {
        setAccessRights(String.format("%s/%s", organization, project), accessRights, credential);
    }

    public void setResolverForProject(String organization, String project, List<String> resolvers, Credential credential) {
        JsonDocument resolver = new JsonDocument();
        resolver.put(JsonLdConsts.TYPE, "CrossProject");
        resolver.put("projects", resolvers);
        JsonDocument identity = new JsonDocument();
        identity.put(JsonLdConsts.TYPE, "Anonymous");
        resolver.put("identities", Collections.singletonList(identity));
        resolver.put("priority", 50);

        try {
            put(new NexusRelativeUrl(NexusConfiguration.ResourceType.RESOLVERS, String.format("%s/%s/schemaResolver", organization, project)), null, resolver, credential);
        } catch (HttpClientErrorException e) {
            Integer expectedRevision = getExpectedRevisionIfIncorrect(e);
            if (expectedRevision != null) {
                put(new NexusRelativeUrl(NexusConfiguration.ResourceType.RESOLVERS, String.format("%s/%s/schemaResolver", organization, project)), expectedRevision, resolver, credential);
            } else {
                throw e;
            }
        }
    }


    private void setAccessRights(String relativeUrl, List<ACLEntry> accessRights, Credential credential) {
        List<JsonDocument> elements = new ArrayList<>();
        for (ACLEntry accessRight : accessRights) {
            JsonDocument unit = aclentryToJson(accessRight);
            elements.add(unit);
        }
        JsonDocument acl = new JsonDocument();
        acl.put("acl", elements);
        putWithRetryForRevision(relativeUrl, credential, acl);
    }

    private void putWithRetryForRevision(String relativeUrl, Credential credential, JsonDocument acl) {
        try {
            put(new NexusRelativeUrl(NexusConfiguration.ResourceType.IAM, relativeUrl), null, acl, credential);
        } catch (HttpClientErrorException e) {
            Integer expectedRevision = getExpectedRevisionIfIncorrect(e);
            if (expectedRevision != null) {
                put(new NexusRelativeUrl(NexusConfiguration.ResourceType.IAM, relativeUrl), expectedRevision, acl, credential);
            } else {
                throw e;
            }
        }
    }

    private JsonDocument aclentryToJson(ACLEntry accessRight) {
        JsonDocument unit = new JsonDocument();
        JsonDocument identity = new JsonDocument();
        switch (accessRight.getType()) {
            case TYPE:
                identity.put("@type", accessRight.getValue());
                break;
            case GROUP:
                identity.put("group", accessRight.getValue());
                break;
            case SUBJECT:
                identity.put("subject", accessRight.getValue());
                break;
        }
        identity.put("realm", accessRight.getRealm());
        unit.put("identity", identity);
        unit.put("permissions", accessRight.getPermissions());
        return unit;
    }

    private boolean isSameIdentity(Map unit, ACLEntry aclEntry) {
        Map identity = (Map) unit.get("identity");
        if ((aclEntry.getRealm() == null && identity.get("realm") == null) || aclEntry.getRealm().equals(identity.get("realm"))) {
            switch (aclEntry.getType()) {
                case GROUP:
                    if (aclEntry.getValue().equals(identity.get("group"))) {
                        return true;
                    }
                    break;
                case SUBJECT:
                    if (aclEntry.getValue().equals(identity.get("subject"))) {
                        return true;
                    }
                    break;
                case TYPE:
                    if (aclEntry.getValue().equals(identity.get(JsonLdConsts.TYPE))) {
                        return true;
                    }
            }
        }
        return false;
    }

    public void addAccessRightForOrganization(String organization, ACLEntry aclEntry, Credential credential) {
        JsonDocument jsonDocument = get(new NexusRelativeUrl(NexusConfiguration.ResourceType.IAM, String.format("%s?self=false", organization)), credential);
        List<Map> map = (List<Map>) jsonDocument.get("_results");
        List<Map> acl;
        boolean updated = false;
        if (map.size() == 1) {
            Map acls = map.get(0);
            if (acls == null) {
                acl = new ArrayList<>();
            } else {
                acl = (List<Map>) acls.get("acl");
            }
        } else {
            acl = new ArrayList<>();
        }
        for (Map unit : acl) {
            if (isSameIdentity(unit, aclEntry)) {
                unit.put("permissions", aclEntry.getPermissions());
                updated = true;
            }
        }
        if (!updated) {
            acl.add(aclentryToJson(aclEntry));
        }
        JsonDocument payload = new JsonDocument();
        payload.put("acl", acl);
        putWithRetryForRevision(organization, credential, payload);
    }

    public void setAccessRightsForOrganization(String organization, List<String> readGroups, Credential credential) {
        List<ACLEntry> aclEntries = new ArrayList<>();
        for (String readGroup : readGroups) {
            aclEntries.add(new ACLEntry(ACLEntry.Type.GROUP, readGroup, defaultRealm, "organizations/read"));
        }
        setAccessRights(organization, aclEntries, credential);
    }

    private Integer getExpectedRevisionIfIncorrect(HttpClientErrorException e) {
        if ("IncorrectRev".equals(getExceptionType(e))) {
            Map map = jsonTransformer.parseToMap(e.getResponseBodyAsString());
            Double expected = (Double) map.get("expected");
            return expected.intValue();
        }
        return null;
    }


    private RestTemplate createRestTemplate(ClientHttpRequestInterceptor oidc) {
        RestTemplate template = new RestTemplate();
        template.setInterceptors(Collections.singletonList(oidc));
        template.getMessageConverters().add(0, new StringHttpMessageConverter(StandardCharsets.UTF_8));
        return template;
    }


    public Set<String> getAllOrganizations(ClientHttpRequestInterceptor oidc) {
        List<JsonDocument> list = list(new NexusRelativeUrl(NexusConfiguration.ResourceType.ORGANIZATION, "?size=100"), oidc, true);
        return list.stream().map(org -> org.get("resultId").toString()).collect(Collectors.toSet());
    }

    public Set<NexusSchemaReference> getAllSchemas(String org, String domain, ClientHttpRequestInterceptor oidc) {
        String relativePath = "";
        if (org != null) {
            relativePath = "/" + org;
        }
        if (domain != null) {
            relativePath = relativePath + "/" + domain;
        }

        List<JsonDocument> schemas = list(new NexusRelativeUrl(NexusConfiguration.ResourceType.SCHEMA, relativePath + "?size=100"), oidc, true);
        return schemas.stream().map(schema -> NexusSchemaReference.createFromUrl(schema.get("resultId").toString())).collect(Collectors.toSet());
    }

    public JsonDocument getUserInfo(Credential credential) {
        return new JsonDocument(createRestTemplate(authorizationController.getInterceptor(credential)).getForEntity(configuration.getUserInfoEndpoint(), Map.class).getBody());

    }

    public JsonDocument put(NexusRelativeUrl url, Integer revision, Map payload, Credential oidc) {
        return put(url, revision, payload, authorizationController.getInterceptor(oidc));
    }


    public JsonDocument put(NexusRelativeUrl url, Integer revision, Map payload, ClientHttpRequestInterceptor oidc) {
        try {
            String endpoint = String.format("%s%s", configuration.getNexusEndpoint(url), revision != null ? String.format("%srev=%d", !url.getUrl().contains("?") ? "?" : "&", revision) : "");
            ResponseEntity<Map> result = createRestTemplate(oidc).exchange(endpoint, HttpMethod.PUT, new HttpEntity<>(payload), Map.class);
            if (result.getStatusCode().is2xxSuccessful() && result.getBody() != null) {
                return new JsonDocument(result.getBody());
            }
            return null;
        } catch (HttpClientErrorException e) {
            logger.debug(e.getMessage(), e);
            throw e;
        }
    }

    public boolean delete(NexusRelativeUrl url, Integer revision, Credential credential) {
        return delete(url, revision, authorizationController.getInterceptor(credential));
    }

    public boolean delete(NexusRelativeUrl url, Integer revision, ClientHttpRequestInterceptor oidc) {
        try {
            createRestTemplate(oidc).delete(String.format("%s%s", configuration.getNexusEndpoint(url), revision != null ? String.format("%srev=%d", !url.getUrl().contains("?") ? "?" : "&", revision) : ""));
            return true;
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.CONFLICT) {
                logger.info("Was not able to remove the instance {} due to a conflict. It seems as it is already deprecated", url.getUrl());
                return false;
            } else {
                logger.error("Was not able to delete the instance", e);
            }

        }
        return false;
    }

    public JsonDocument post(NexusRelativeUrl url, Integer revision, Map payload, Credential credential) {
        return post(url, revision, payload, authorizationController.getInterceptor(credential));
    }

    public JsonDocument post(NexusRelativeUrl url, Integer revision, Map payload, ClientHttpRequestInterceptor oidc) {
        try {
            ResponseEntity<Map> result = createRestTemplate(oidc).exchange(String.format("%s%s", configuration.getNexusEndpoint(url), revision != null ? String.format("%srev=%d", !url.getUrl().contains("?") ? "?" : "&", revision) : ""), HttpMethod.POST, new HttpEntity<>(payload), Map.class);
            if (result.getStatusCode().is2xxSuccessful() && result.getBody() != null) {
                return new JsonDocument(result.getBody());
            }
        } catch (HttpClientErrorException e) {
            logger.error("Was not able to create instance in nexus", e.getResponseBodyAsString());
            throw e;
        }
        return null;
    }


    public JsonDocument patch(NexusRelativeUrl url, Integer revision, Map payload, Credential credential) {
        return patch(url, revision, payload, authorizationController.getInterceptor(credential));
    }

    JsonDocument patch(NexusRelativeUrl url, Integer revision, Map payload, ClientHttpRequestInterceptor oidc) {
        RestTemplate template = createRestTemplate(oidc);
        template.setRequestFactory(new HttpComponentsClientHttpRequestFactory());
        ResponseEntity<Map> result = template.exchange(String.format("%s%s", configuration.getNexusEndpoint(url), revision != null ? String.format("%srev=%d", !url.getUrl().contains("?") ? "?" : "&", revision) : ""), HttpMethod.PATCH, new HttpEntity<>(payload), Map.class);
        if (result.getStatusCode().is2xxSuccessful() && result.getBody() != null) {
            return new JsonDocument(result.getBody());
        }
        return null;
    }

    public List<JsonDocument> find(NexusSchemaReference nexusSchemaReference, String fieldName, String fieldValue, Credential credential) {
        return find(nexusSchemaReference, fieldName, fieldValue, authorizationController.getInterceptor(credential));
    }

    List<JsonDocument> find(NexusSchemaReference nexusSchemaReference, String fieldName, String fieldValue, ClientHttpRequestInterceptor oidc) {
        NexusRelativeUrl relativeUrl = new NexusRelativeUrl(NexusConfiguration.ResourceType.RESOURCES, nexusSchemaReference.getRelativeUrl().getUrl());
        relativeUrl.addQueryParameter("fields", "all");
        relativeUrl.addQueryParameter("deprecated", "false");
        relativeUrl.addQueryParameter("filter", String.format("{\"op\":\"eq\",\"path\":\"%s\",\"value\":\"%s\"}", fieldName, fieldValue));
        String url = configuration.getNexusEndpoint(relativeUrl);
        ResponseEntity<Map> result = createRestTemplate(oidc).getForEntity(url, Map.class);
        if (result.getStatusCode().is2xxSuccessful() && result.getBody() != null && result.getBody().containsKey("results") && result.getBody().get("results") instanceof List) {
            return (List<JsonDocument>) ((List) result.getBody().get("results")).stream().filter(r -> r instanceof Map).map(r -> new JsonDocument((Map) r)).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }


    public JsonDocument get(NexusRelativeUrl url, Credential credential) {
        return get(url, authorizationController.getInterceptor(credential));
    }

    JsonDocument get(NexusRelativeUrl url, ClientHttpRequestInterceptor oidc) {
        Map map = get(url, oidc, Map.class);
        return map != null ? new JsonDocument(map) : null;
    }

    public <T> T get(NexusRelativeUrl url, Credential credential, Class<T> resultClass) {
        return get(url, authorizationController.getInterceptor(credential), resultClass);
    }

    <T> T get(NexusRelativeUrl url, ClientHttpRequestInterceptor oidc, Class<T> resultClass) {
        try {
            ResponseEntity<T> result = createRestTemplate(oidc).getForEntity(configuration.getNexusEndpoint(url), resultClass);
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


    public List<JsonDocument> list(NexusRelativeUrl relativeUrl, Credential credential, boolean followPages) {
        return list(relativeUrl, authorizationController.getInterceptor(credential), followPages);
    }

    public List<JsonDocument> list(NexusSchemaReference schemaReference, Credential credential, boolean followPages) {
        return list(schemaReference, authorizationController.getInterceptor(credential), followPages);
    }

    List<JsonDocument> list(NexusSchemaReference schemaReference, ClientHttpRequestInterceptor oidc, boolean followPages) {
        return list(new NexusRelativeUrl(NexusConfiguration.ResourceType.RESOURCES, schemaReference.getRelativeUrl().getUrl()), oidc, followPages);
    }


    public List<NexusSchemaReference> listSchemasByOrganization(String organization, Credential oidc, boolean followPages) {
        NexusRelativeUrl relativeUrl = new NexusRelativeUrl(NexusConfiguration.ResourceType.SCHEMA, organization);
        List<JsonDocument> list = list(relativeUrl, oidc, followPages);
        return list.stream().map(d -> NexusSchemaReference.createFromUrl((String) d.get("resultId"))).collect(Collectors.toList());
    }

    List<JsonDocument> list(NexusRelativeUrl relativeUrl, ClientHttpRequestInterceptor oidc, boolean followPages) {
        return list(createUrl(relativeUrl), oidc, followPages);
    }

    private String createUrl(NexusRelativeUrl relativeUrl) {
        return configuration.getNexusEndpoint(relativeUrl) + (relativeUrl.getUrl().contains("?") ? "&" : "?") + "deprecated=false";
    }

    public void consumeInstances(NexusSchemaReference schemaReference, Credential credential, boolean followPages, Consumer<List<NexusInstanceReference>> consumer) {
        consume(createUrl(new NexusRelativeUrl(NexusConfiguration.ResourceType.RESOURCES, schemaReference.getRelativeUrl().getUrl())), authorizationController.getInterceptor(credential), followPages, consumer);
    }

    private void consume(String url, ClientHttpRequestInterceptor oidc, boolean followPages, Consumer<List<NexusInstanceReference>> consumer) {
        ResponseEntity<Map> result = createRestTemplate(oidc).getForEntity(url, Map.class);
        if (result.getStatusCode().is2xxSuccessful() && result.getBody() != null && result.getBody().containsKey("results") && result.getBody().get("results") instanceof List) {
            List<JsonDocument> results = (List<JsonDocument>) ((List) result.getBody().get("results")).stream().map(r -> new JsonDocument((Map) r)).collect(Collectors.toList());
            consumer.accept(results.stream().map(r -> NexusInstanceReference.createFromUrl((String) r.get("resultId"))).collect(Collectors.toList()));
            if (followPages) {
                Object links = result.getBody().get("links");
                if (links instanceof Map) {
                    Object next = ((Map) links).get("next");
                    if (next instanceof String) {
                        consume((String) next, oidc, true, consumer);
                    }
                }
            }
        }
    }

    //TODO reimplement with the use of consume
    private List<JsonDocument> list(String url, ClientHttpRequestInterceptor oidc, boolean followPages) {
        ResponseEntity<Map> result = createRestTemplate(oidc).getForEntity(url, Map.class);
        if (result.getStatusCode().is2xxSuccessful() && result.getBody() != null && result.getBody().containsKey("results") && result.getBody().get("results") instanceof List) {
            List<JsonDocument> results = (List<JsonDocument>) ((List) result.getBody().get("results")).stream().map(r -> new JsonDocument((Map) r)).collect(Collectors.toList());
            if (followPages) {
                Object links = result.getBody().get("links");
                if (links instanceof Map) {
                    Object next = ((Map) links).get("next");
                    if (next instanceof String) {
                        List<JsonDocument> nextPage = list((String) next, oidc, true);
                        results.addAll(nextPage);
                    }
                }
            }
            return results;
        }
        return Collections.emptyList();
    }

}
