package org.humanbrainproject.knowledgegraph.instances.api;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiParam;
import org.humanbrainproject.knowledgegraph.commons.InternalApi;
import org.humanbrainproject.knowledgegraph.commons.authorization.entity.OidcAccessToken;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusInstanceReference;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusSchemaReference;
import org.humanbrainproject.knowledgegraph.instances.boundary.Instances;
import org.humanbrainproject.knowledgegraph.instances.entity.Client;
import org.humanbrainproject.knowledgegraph.query.boundary.ArangoGraph;
import org.humanbrainproject.knowledgegraph.query.entity.DatabaseScope;
import org.humanbrainproject.knowledgegraph.query.entity.JsonDocument;
import org.humanbrainproject.knowledgegraph.query.entity.QueryParameters;
import org.humanbrainproject.knowledgegraph.query.entity.QueryResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;

import javax.ws.rs.core.MediaType;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.humanbrainproject.knowledgegraph.commons.api.ParameterConstants.*;

@RestController
@RequestMapping(value = "/internal/api/instances", produces = MediaType.APPLICATION_JSON)
@InternalApi
@Api(value = "/internal/api/instances", description = "The API for managing instances")
public class InstancesInternalAPI {

    @Autowired
    Instances instances;

    @Autowired
    ArangoGraph graph;

    @PostMapping(value = "/{org}/{domain}/{schema}/{version}")
    public ResponseEntity<Map> createNewInstanceForSchema(@RequestBody(required = false) String payload, @PathVariable(ORG) String org, @PathVariable(DOMAIN) String domain, @PathVariable(SCHEMA) String schema, @PathVariable(VERSION) String version, @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationToken, @RequestHeader(value = "client", required = false) Client client) {
        NexusInstanceReference reference = instances.createNewInstance(new NexusSchemaReference(org, domain, schema, version), payload, client, new OidcAccessToken().setToken(authorizationToken));
        if (reference != null) {
            Map<String, String> result = new HashMap<>();
            result.put("relativeUrl", reference.getRelativeUrl().getUrl());
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping(value = "/{org}/{domain}/{schema}/{version}/{id}", consumes = {MediaType.APPLICATION_JSON, "application/ld+json"})
    public ResponseEntity<Map> updateInstance(@RequestBody String payload, @PathVariable(ORG) String org, @PathVariable(DOMAIN) String domain, @PathVariable(SCHEMA) String schema, @PathVariable(VERSION) String version, @PathVariable("id") String id, @ApiParam("The clientIdExtension allows the calling client to specify an additional postfix to the identifier and therefore to discriminate between different instances which are combined in the inferred space. If this value takes a userId for example, this means that there will be a distinct instance created for every user.") @RequestParam(value = "clientIdExtension", required = false) String clientIdExtension, @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationToken, @RequestHeader(value = "client", required = false) Client client) {
        NexusInstanceReference instanceReference = new NexusInstanceReference(org, domain, schema, version, id);
        NexusInstanceReference newReference = instances.updateInstance(instanceReference, payload, client, clientIdExtension, new OidcAccessToken().setToken(authorizationToken));
        if (newReference != null) {
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }


    @PutMapping(value = "/{org}/{domain}/{schema}/{version}/reindex")
    public ResponseEntity<Void> reindexInstancesFromSchema(@PathVariable(ORG) String org, @PathVariable(DOMAIN) String domain, @PathVariable(SCHEMA) String schema, @PathVariable("version") String version, @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationToken) {
        instances.reindexInstancesFromSchema(new NexusSchemaReference(org, domain, schema, version), new OidcAccessToken().setToken(authorizationToken));
        return ResponseEntity.ok().build();
    }


    @PutMapping(value = "/{org}/{domain}/{schema}/{oldVersion}/clone/{newVersion}")
    public ResponseEntity<Void> cloneInstancesFromSchema(@PathVariable(ORG) String org, @PathVariable(DOMAIN) String domain, @PathVariable(SCHEMA) String schema, @PathVariable("oldVersion") String oldVersion, @PathVariable("newVersion") String newVersion, @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationToken) {
        instances.cloneInstancesFromSchema(new NexusSchemaReference(org, domain, schema, oldVersion), newVersion, new OidcAccessToken().setToken(authorizationToken));
        return ResponseEntity.ok().build();
    }

    @PutMapping(value = "/{org}/{domain}/{schema}/{version}/namespaces")
    public ResponseEntity<Void> translateNamespacesForSchema(@PathVariable(ORG) String org, @PathVariable(DOMAIN) String domain, @PathVariable(SCHEMA) String schema, @PathVariable(VERSION) String version, @RequestHeader(value = "oldNamespace") String oldNamespace, @RequestHeader(value = "newNamespace") String newNamespace, @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationToken) {
        instances.translateNamespaces(new NexusSchemaReference(org, domain, schema, version),  oldNamespace, newNamespace, new OidcAccessToken().setToken(authorizationToken));
        return ResponseEntity.ok().build();
    }

    @GetMapping(value = "/{org}/{domain}/{schema}/{version}/identifier/{identifier}")
    public ResponseEntity<Map> getInstance(@PathVariable(ORG) String org, @PathVariable(DOMAIN) String domain, @PathVariable(SCHEMA) String schema, @PathVariable(VERSION) String version, @PathVariable("identifier") String identifier, @RequestParam(value = DATABASE_SCOPE, required = false) DatabaseScope databaseScope, @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationToken) throws Exception {
        try {
            JsonDocument instanceByIdentifier = instances.findInstanceByIdentifier(new NexusSchemaReference(org, domain, schema, version), identifier, databaseScope != null ? databaseScope : DatabaseScope.INFERRED, new OidcAccessToken().setToken(authorizationToken));
            return instanceByIdentifier != null ? ResponseEntity.ok(instanceByIdentifier) : ResponseEntity.notFound().build();
        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).build();
        }
    }


    @GetMapping(value = "/{org}/{domain}/{schema}/{version}/{id}")
    public ResponseEntity<Map> getInstance(@PathVariable(ORG) String org, @PathVariable(DOMAIN) String domain, @PathVariable(SCHEMA) String schema, @PathVariable(VERSION) String version, @PathVariable("id") String id, @ApiParam("Defines the database scope. This is reset to NATIVE, if a client / client extension is defined") @RequestParam(value = DATABASE_SCOPE, required = false) DatabaseScope databaseScope, @ApiParam("The clientIdExtension allows the calling client to specify an additional postfix to the identifier and therefore to discriminate between different instances which are combined in the inferred space. If this value takes a userId for example, this means that there will be a distinct instance created for every user.") @RequestParam(value = "clientIdExtension", required = false) String clientIdExtension, @RequestHeader(value = "client", required = false) Client client, @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationToken) throws Exception {
        try {
            NexusInstanceReference instanceReference = new NexusInstanceReference(org, domain, schema, version, id);
            OidcAccessToken credential = new OidcAccessToken().setToken(authorizationToken);
            Map instance;
            if(clientIdExtension!=null || client!=null){
                databaseScope = DatabaseScope.NATIVE;
            }
            if(databaseScope==null){
                databaseScope = DatabaseScope.INFERRED;
            }
            if(clientIdExtension!=null){
                instance = instances.getInstanceByClientExtension(instanceReference, clientIdExtension, client, credential);
            }
            else{
                instance = instances.getInstance(instanceReference, databaseScope, credential);
            }
            return instance != null ? ResponseEntity.ok(instance) : ResponseEntity.notFound().build();
        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).build();
        }
    }

    @GetMapping(value = "/{org}/{domain}/{schema}/{version}")
    public ResponseEntity<QueryResult<List<Map>>> getInstances(@PathVariable(ORG) String org, @PathVariable(DOMAIN) String domain, @PathVariable(SCHEMA) String schema, @PathVariable(VERSION) String version, @RequestParam(value = SIZE, required = false) Integer size, @RequestParam(value = START, required = false) Integer start, @RequestParam(value = DATABASE_SCOPE, required = false) DatabaseScope databaseScope, @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationToken) throws Exception {
        try {
            NexusSchemaReference schemaReference = new NexusSchemaReference(org, domain, schema, version);
            OidcAccessToken credential = new OidcAccessToken().setToken(authorizationToken);
            QueryParameters parameters = new QueryParameters(databaseScope, Collections.emptyMap());
            parameters.pagination().setStart(start).setSize(size);
            QueryResult<List<Map>> instances = this.instances.getInstances(schemaReference, parameters, credential);
            return instances != null ? ResponseEntity.ok(instances) : ResponseEntity.notFound().build();
        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).build();
        }
    }

}

