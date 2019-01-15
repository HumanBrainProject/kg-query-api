package org.humanbrainproject.knowledgegraph;

import org.humanbrainproject.knowledgegraph.commons.authorization.entity.Credential;
import org.humanbrainproject.knowledgegraph.commons.jsonld.control.JsonTransformer;
import org.humanbrainproject.knowledgegraph.commons.nexus.control.NexusClient;
import org.humanbrainproject.knowledgegraph.commons.nexus.control.NexusConfiguration;
import org.humanbrainproject.knowledgegraph.indexing.api.IndexingInternalAPI;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusInstanceReference;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusRelativeUrl;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusSchemaReference;
import org.humanbrainproject.knowledgegraph.query.entity.JsonDocument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.Collectors;


@Component
public class NexusMockClient extends NexusClient {

    Set<NexusSchemaReference> schemas = new HashSet<>();
    Map<NexusRelativeUrl, Map> documents = new LinkedHashMap<>();
    Map<NexusRelativeUrl, Integer> revisions = new HashMap<>();
    JsonTransformer jsonTransformer = new JsonTransformer();

    @Autowired
    IndexingInternalAPI indexingAPI;


    @Override
    public Set<String> getAllOrganizations(ClientHttpRequestInterceptor oidc) {
        return schemas.stream().map(s -> s.getOrganization()).collect(Collectors.toSet());
    }

    @Override
    public Set<NexusSchemaReference> getAllSchemas(String org, String domain, ClientHttpRequestInterceptor oidc) {
        return schemas;
    }

    private void updateDocument(NexusRelativeUrl url, Integer revision, Map payload){
        documents.put(url, payload);
        revisions.put(url, revision);
    }


    @Async
    public CompletableFuture<ResponseEntity<String>> indexInstanceUpdate(NexusInstanceReference reference, Map payload) {
        ResponseEntity<String> result = indexingAPI.updateInstance(jsonTransformer.getMapAsJson(payload), reference.getNexusSchema().getOrganization(), reference.getNexusSchema().getDomain(), reference.getNexusSchema().getSchema(), reference.getNexusSchema().getSchemaVersion(), reference.getId(), reference.getRevision(), "author", ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT));
        return CompletableFuture.completedFuture(result);
    }

    @Async
    public CompletableFuture<ResponseEntity<String>> indexInstanceDeletion(NexusInstanceReference reference) {
        ResponseEntity<String> result = indexingAPI.deleteInstance(reference.getNexusSchema().getOrganization(), reference.getNexusSchema().getDomain(), reference.getNexusSchema().getSchema(), reference.getNexusSchema().getSchemaVersion(), reference.getId(), reference.getRevision(), "author", ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT));
        return CompletableFuture.completedFuture(result);
    }

    @Async
    public CompletableFuture<ResponseEntity<String>> indexInstanceInsertion(NexusInstanceReference reference, Map payload) {
        ResponseEntity<String> result = indexingAPI.addInstance(jsonTransformer.getMapAsJson(payload), reference.getNexusSchema().getOrganization(), reference.getNexusSchema().getDomain(), reference.getNexusSchema().getSchema(), reference.getNexusSchema().getSchemaVersion(), reference.getId(), "author", ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT));
        return CompletableFuture.completedFuture(result);
    }


    @Override
    public JsonDocument put(NexusRelativeUrl url, Integer revision, Map payload, Credential oidc) {
       updateDocument(url, revision, payload);
        //TODO simulate payload changes
        if(url.getResourceType()== NexusConfiguration.ResourceType.DATA) {
            NexusInstanceReference fromUrl = NexusInstanceReference.createFromUrl(url.getUrl());
            fromUrl.setRevision(revision);
            //trigger indexing
            indexInstanceUpdate(fromUrl, payload);
        }

        return new JsonDocument(payload);
    }

    @Override
    public JsonDocument put(NexusRelativeUrl url, Integer revision, Map payload, ClientHttpRequestInterceptor oidc) {
       return this.put(url, revision, payload, (Credential)null);
    }

    @Override
    public boolean delete(NexusRelativeUrl url, Integer revision, Credential credential) {
        documents.remove(url);
        revisions.remove(url);
        if(url.getResourceType()== NexusConfiguration.ResourceType.DATA) {
            NexusInstanceReference fromUrl = NexusInstanceReference.createFromUrl(url.getUrl());
            fromUrl.setRevision(revision);
            //trigger indexing
            indexInstanceDeletion(fromUrl);
        }
        return true;
    }

    @Override
    public boolean delete(NexusRelativeUrl url, Integer revision, ClientHttpRequestInterceptor oidc) {
        delete(url, revision, (Credential)null);
        return true;
    }

    @Override
    public JsonDocument post(NexusRelativeUrl url, Integer revision, Map payload, Credential credential) {
        updateDocument(url, revision, payload);
        //TODO simulate payload changes
        if(url.getResourceType()== NexusConfiguration.ResourceType.DATA) {
            NexusInstanceReference fromUrl = NexusInstanceReference.createFromUrl(url.getUrl());
            //trigger indexing
            indexInstanceInsertion(fromUrl, payload);
        }
        return new JsonDocument(payload);
    }

    @Override
    public JsonDocument post(NexusRelativeUrl url, Integer revision, Map payload, ClientHttpRequestInterceptor oidc) {
        return post(url, revision, payload, (Credential)null);
    }

    @Override
    public JsonDocument patch(NexusRelativeUrl url, Integer revision, Map payload, Credential credential) {
        //We ignore the schema publication
        return new JsonDocument(payload);
    }

    @Override
    public List<JsonDocument> find(NexusSchemaReference nexusSchemaReference, String fieldName, String fieldValue, Credential credential) {
        List<JsonDocument> list = list(nexusSchemaReference, credential, true);
        return list.stream().filter(d -> d.containsKey(fieldName) && fieldValue.equals(d.get(fieldName))).collect(Collectors.toList());
    }

    @Override
    public JsonDocument get(NexusRelativeUrl url, Credential credential) {
        return new JsonDocument(documents.get(url));
    }

    @Override
    public <T> T get(NexusRelativeUrl url, Credential credential, Class<T> resultClass) {
        if(resultClass==String.class){
            return (T) new JsonTransformer().getMapAsJson(get(url, credential));
        }
        else if(resultClass==Map.class){
            return (T) documents.get(url);
        }
        throw new IllegalArgumentException("Was not able to transform a document into class "+resultClass);
    }

    @Override
    public List<JsonDocument> list(NexusRelativeUrl relativeUrl, Credential credential, boolean followPages) {
        Set<NexusRelativeUrl> matchingKeys = documents.keySet().stream().filter(s -> s.getUrl().startsWith(relativeUrl.getUrl())).collect(Collectors.toSet());
        return (List<JsonDocument>)matchingKeys.stream().map(k -> documents.get(k)).map(JsonDocument::new).collect(Collectors.toList());
    }

    @Override
    public List<JsonDocument> list(NexusSchemaReference schemaReference, Credential credential, boolean followPages) {
        return list(schemaReference.getRelativeUrl(), credential, followPages);
    }

    @Override
    public List<NexusSchemaReference> listSchemasByOrganization(String organization, Credential oidc, boolean followPages) {
        return schemas.stream().filter(s -> s.getRelativeUrl().getUrl().startsWith(organization)).collect(Collectors.toList());
    }

    @Override
    public void consumeInstances(NexusSchemaReference schemaReference, Credential credential, boolean followPages, Consumer<List<NexusInstanceReference>> consumer) {
        List<JsonDocument> list = list(schemaReference, credential, followPages);
        List<NexusInstanceReference> references = list.stream().map(d -> NexusInstanceReference.createFromUrl(d.getNexusId())).collect(Collectors.toList());
        consumer.accept(references);
    }
}
