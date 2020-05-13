/*
 * Copyright 2020 EPFL/Human Brain Project PCO
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.humanbrainproject.knowledgegraph.instances.api;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiParam;
import org.humanbrainproject.knowledgegraph.annotations.ToBeTested;
import org.humanbrainproject.knowledgegraph.commons.InternalApi;
import org.humanbrainproject.knowledgegraph.commons.api.Client;
import org.humanbrainproject.knowledgegraph.commons.api.RestUtils;
import org.humanbrainproject.knowledgegraph.commons.authorization.control.AuthorizationContext;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.ArangoInferredRepository;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.ArangoNativeRepository;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.entity.SubSpace;
import org.humanbrainproject.knowledgegraph.commons.vocabulary.ArangoVocabulary;
import org.humanbrainproject.knowledgegraph.commons.vocabulary.HBPVocabulary;
import org.humanbrainproject.knowledgegraph.context.QueryContext;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusInstanceReference;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusSchemaReference;
import org.humanbrainproject.knowledgegraph.instances.boundary.Instances;
import org.humanbrainproject.knowledgegraph.instances.control.InstanceLookupController;
import org.humanbrainproject.knowledgegraph.query.boundary.ArangoGraph;
import org.humanbrainproject.knowledgegraph.query.entity.DatabaseScope;
import org.humanbrainproject.knowledgegraph.query.entity.JsonDocument;
import org.humanbrainproject.knowledgegraph.query.entity.Pagination;
import org.humanbrainproject.knowledgegraph.query.entity.QueryResult;
import org.humanbrainproject.knowledgegraph.releasing.entity.ReleaseStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.MediaType;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.humanbrainproject.knowledgegraph.commons.api.ParameterConstants.*;

@RestController
@RequestMapping(value = "/internal/api/instances", produces = MediaType.APPLICATION_JSON)
@InternalApi
@Api(value = "/internal/api/instances", description = "The API for managing instances")
@ToBeTested(easy = true)
public class InstancesInternalAPI {

    @Autowired
    AuthorizationContext authorizationContext;

    @Autowired
    QueryContext queryContext;

    @Autowired
    Instances instances;

    @Autowired
    ArangoGraph graph;

    @Autowired
    ArangoNativeRepository arangoNativeRepository;

    @Autowired
    ArangoInferredRepository arangoInferredRepository;


    @PostMapping(value = "/{"+ORG+"}/{"+DOMAIN+"}/{"+SCHEMA+"}/{"+VERSION+"}")
    public ResponseEntity<Map> createNewInstanceForSchema(@RequestBody(required = false) String payload, @PathVariable(ORG) String org, @PathVariable(DOMAIN) String domain, @PathVariable(SCHEMA) String schema, @PathVariable(VERSION) String version, @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationToken, @RequestHeader(value = CLIENT, required = false) Client client, @ApiParam(CLIENT_EXTENSION_DOC) @RequestParam(value = CLIENT_ID_EXTENSION, required = false) String clientIdExtension) {
        authorizationContext.populateAuthorizationContext(authorizationToken, client);
        NexusInstanceReference reference = instances.createNewInstance(new NexusSchemaReference(org, domain, schema, version), payload, clientIdExtension);
        if (reference != null) {
            Map<String, String> result = new HashMap<>();
            result.put("relativeUrl", reference.getRelativeUrl().getUrl());
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping(value = "/{"+ORG+"}/{"+DOMAIN+"}/{"+SCHEMA+"}/{"+VERSION+"}/{"+ID+"}", consumes = {MediaType.APPLICATION_JSON, RestUtils.APPLICATION_LD_JSON})
    public ResponseEntity<Map> updateInstance(@RequestBody String payload, @PathVariable(ORG) String org, @PathVariable(DOMAIN) String domain, @PathVariable(SCHEMA) String schema, @PathVariable(VERSION) String version, @PathVariable(ID) String id, @ApiParam(CLIENT_EXTENSION_DOC) @RequestParam(value = CLIENT_ID_EXTENSION, required = false) String clientIdExtension, @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationToken, @RequestHeader(value = CLIENT, required = false) Client client) {
        authorizationContext.populateAuthorizationContext(authorizationToken, client);
        NexusInstanceReference instanceReference = new NexusInstanceReference(org, domain, schema, version, id);
        NexusInstanceReference newReference = instances.updateInstance(instanceReference, payload, clientIdExtension);
        if (newReference != null) {
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }


    @PutMapping(value = "/{"+ORG+"}/{"+DOMAIN+"}/{"+SCHEMA+"}/{"+VERSION+"}/reindex")
    public ResponseEntity<Void> reindexInstancesFromSchema(@PathVariable(ORG) String org, @PathVariable(DOMAIN) String domain, @PathVariable(SCHEMA) String schema, @PathVariable(VERSION) String version, @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationToken) {
        authorizationContext.populateAuthorizationContext(authorizationToken);
        instances.reindexInstancesFromSchema(new NexusSchemaReference(org, domain, schema, version));
        return ResponseEntity.ok().build();
    }


    @PutMapping(value = "/{"+ORG+"}/{"+DOMAIN+"}/{"+SCHEMA+"}/{oldVersion}/clone/{newVersion}")
    public ResponseEntity<Void> cloneInstancesFromSchema(@PathVariable(ORG) String org, @PathVariable(DOMAIN) String domain, @PathVariable(SCHEMA) String schema, @PathVariable("oldVersion") String oldVersion, @PathVariable("newVersion") String newVersion, @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationToken) {
        authorizationContext.populateAuthorizationContext(authorizationToken);
        instances.cloneInstancesFromSchema(new NexusSchemaReference(org, domain, schema, oldVersion), newVersion);
        return ResponseEntity.ok().build();
    }

    @PutMapping(value = "/{"+ORG+"}/{"+DOMAIN+"}/{"+SCHEMA+"}/{"+VERSION+"}/namespaces")
    public ResponseEntity<Void> translateNamespacesForSchema(@PathVariable(ORG) String org, @PathVariable(DOMAIN) String domain, @PathVariable(SCHEMA) String schema, @PathVariable(VERSION) String version, @RequestHeader(value = "oldNamespace") String oldNamespace, @RequestHeader(value = "newNamespace") String newNamespace, @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationToken) {
        authorizationContext.populateAuthorizationContext(authorizationToken);
        instances.translateNamespaces(new NexusSchemaReference(org, domain, schema, version), oldNamespace, newNamespace);
        return ResponseEntity.ok().build();
    }

    @GetMapping(value = "/{"+ORG+"}/{"+DOMAIN+"}/{"+SCHEMA+"}/{"+VERSION+"}/identifier/{identifier}")
    public ResponseEntity<Map> getInstance(@PathVariable(ORG) String org, @PathVariable(DOMAIN) String domain, @PathVariable(SCHEMA) String schema, @PathVariable(VERSION) String version, @PathVariable("identifier") String identifier, @RequestParam(value = DATABASE_SCOPE, required = false) DatabaseScope databaseScope, @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationToken) {
        try {
            authorizationContext.populateAuthorizationContext(authorizationToken);

            //We set the database scope directly, because this is an internal API and therefore it is allowed to have a "Native" scope as well.
            queryContext.setDatabaseScope(databaseScope);

            JsonDocument instanceByIdentifier = instances.findInstanceByIdentifier(new NexusSchemaReference(org, domain, schema, version), identifier);
            return instanceByIdentifier != null ? ResponseEntity.ok(instanceByIdentifier) : ResponseEntity.notFound().build();
        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).build();
        }
    }


    @GetMapping(value = "/{"+ORG+"}/{"+DOMAIN+"}/{"+SCHEMA+"}/{"+VERSION+"}/{"+ID+"}")
    public ResponseEntity<Map> getInstance(@PathVariable(ORG) String org, @PathVariable(DOMAIN) String domain, @PathVariable(SCHEMA) String schema, @PathVariable(VERSION) String version, @PathVariable(ID) String id, @ApiParam(DATABASE_SCOPE_DOC) @RequestParam(value = DATABASE_SCOPE, required = false) DatabaseScope databaseScope, @ApiParam(CLIENT_EXTENSION_DOC) @RequestParam(value = CLIENT_ID_EXTENSION, required = false) String clientIdExtension, @RequestHeader(value = CLIENT, required = false) Client client, @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationToken) {
        try {
            authorizationContext.populateAuthorizationContext(authorizationToken, client);

            //We set the database scope directly, because this is an internal API and therefore it is allowed to have a "Native" scope as well.
            queryContext.setDatabaseScope(databaseScope);
            NexusInstanceReference instanceReference = new NexusInstanceReference(org, domain, schema, version, id);
            Map instance;
            if(clientIdExtension!=null){
                instance = instances.getInstanceByClientExtension(instanceReference, clientIdExtension);
            }
            else{
                instance = instances.getInstance(instanceReference);
            }
            return instance != null ? ResponseEntity.ok(instance) : ResponseEntity.notFound().build();
        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).build();
        }
    }

    @GetMapping(value = "/{"+ORG+"}/{"+DOMAIN+"}/{"+SCHEMA+"}/{"+VERSION+"}/{"+ID+"}/links/{linked"+ORG+"}/{linked"+ DOMAIN+"}/{linked"+SCHEMA+"}/{linked"+VERSION+"}/{linked"+ID+"}/{link"+ORG+"}/{link"+DOMAIN+"}/{link"+SCHEMA+"}/{link"+VERSION+"}")
    public ResponseEntity<List<Map>> getLinkingInstances(@PathVariable(ORG) String org, @PathVariable(DOMAIN) String domain, @PathVariable(SCHEMA) String schema, @PathVariable(VERSION) String version, @PathVariable(ID) String id, @PathVariable("linked"+ORG) String linkedOrg, @PathVariable("linked"+DOMAIN) String linkedDomain, @PathVariable("linked"+SCHEMA) String linkedSchema, @PathVariable("linked"+VERSION) String linkedVersion, @PathVariable("linked"+ID) String linkedId, @PathVariable("link"+ORG) String linkOrg, @PathVariable("link"+DOMAIN) String linkDomain, @PathVariable("link"+SCHEMA) String linkSchema, @PathVariable("link"+VERSION) String linkVersion, @ApiParam(DATABASE_SCOPE_DOC) @RequestParam(value = DATABASE_SCOPE, required = false) DatabaseScope databaseScope, @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationToken) {
        try {
            authorizationContext.populateAuthorizationContext(authorizationToken);

            //We set the database scope directly, because this is an internal API and therefore it is allowed to have a "Native" scope as well.
            queryContext.setDatabaseScope(databaseScope);

            NexusInstanceReference instanceReference = new NexusInstanceReference(org, domain, schema, version, id);
            NexusInstanceReference linkedInstanceReference = new NexusInstanceReference(linkedOrg, linkedDomain, linkedSchema, linkedVersion, linkedId);
            NexusSchemaReference nexusSchemaReference = new NexusSchemaReference(linkOrg, linkDomain, linkSchema, linkVersion);
            List<Map> linkingInstances = instances.getLinkingInstances(instanceReference, linkedInstanceReference, nexusSchemaReference);
            return linkingInstances != null ? ResponseEntity.ok(linkingInstances) : ResponseEntity.ok(Collections.emptyList());
        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).build();
        }
    }

    @GetMapping(value = "/{"+ORG+"}/{"+DOMAIN+"}/{"+SCHEMA+"}/{"+VERSION+"}")
    public ResponseEntity<QueryResult<List<Map>>> getInstances(@PathVariable(ORG) String org, @PathVariable(DOMAIN) String domain, @PathVariable(SCHEMA) String schema, @PathVariable(VERSION) String version, @RequestParam(value = SIZE, required = false) Integer size, @RequestParam(value = START, required = false) Integer start, @RequestParam(value = SEARCH, required = false) String searchTerm,  @RequestParam(value = DATABASE_SCOPE, required = false) DatabaseScope databaseScope, @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationToken) {
        try {
            authorizationContext.populateAuthorizationContext(authorizationToken);

            //We set the database scope directly, because this is an internal API and therefore it is allowed to have a "Native" scope as well.
            queryContext.setDatabaseScope(databaseScope);

            NexusSchemaReference schemaReference = new NexusSchemaReference(org, domain, schema, version);
            QueryResult<List<Map>> instances = this.instances.getInstances(schemaReference, searchTerm, new Pagination().setSize(size).setStart(start));
            return instances != null ? ResponseEntity.ok(instances) : ResponseEntity.ok(QueryResult.createEmptyResult(queryContext.getDatabaseScope().name()));
        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).build();
        }
    }

    @DeleteMapping(value = "/{"+ORG+"}/{"+DOMAIN+"}/{"+SCHEMA+"}/{"+VERSION+"}/{"+ID+"}")
    public ResponseEntity<QueryResult<List<Map>>> deleteInstance(@PathVariable(ORG) String org, @PathVariable(DOMAIN) String domain, @PathVariable(SCHEMA) String schema, @PathVariable(VERSION) String version, @PathVariable(ID) String id,  @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationToken) {
        try {
            authorizationContext.populateAuthorizationContext(authorizationToken);

            //We set the database scope directly, because this is an internal API and therefore it is allowed to have a "Native" scope as well.
            queryContext.setDatabaseScope(DatabaseScope.INFERRED);
            NexusInstanceReference instanceReference = new NexusInstanceReference(org, domain, schema, version, id);
            JsonDocument inferred = this.instances.getInstance(instanceReference);
            if(inferred != null) {
                NexusInstanceReference originalId = arangoNativeRepository.findOriginalId(instanceReference);
                queryContext.setDatabaseScope(DatabaseScope.RELEASED);
                JsonDocument released = this.instances.getInstance(instanceReference);
                if(released == null){
                    List<Map> links = arangoInferredRepository.getIncomingLinks(instanceReference);
                    List<Map> linksReleased = links.stream().filter(new Predicate<Map>() {
                                                                   @Override
                                                                   public boolean test(Map map) {
                                                                       return map.get("status").equals(ReleaseStatus.RELEASED.name());
                                                                   }
                                                               }

                    ).collect(Collectors.toList());
                    if(linksReleased.isEmpty()){
                        //This means we can delete the original instance as it is not released
                        queryContext.setDatabaseScope(DatabaseScope.NATIVE);
                        if(this.instances.removeInstance(originalId)){
                            return  ResponseEntity.ok(QueryResult.createEmptyResult(queryContext.getDatabaseScope().name()));
                        }
                    }else{
                        List<String> ids = linksReleased.stream().map(l -> (String)((Map)l.get("doc")).get(HBPVocabulary.RELATIVE_URL_OF_INTERNAL_LINK)).collect(Collectors.toList());
                        throw new BadRequestException("The instance is linked to one or more released instance: " + String.join(" ", ids ));
                    }

                    throw  new InternalServerErrorException("Could not delete instance");
                } else {
                    throw new BadRequestException("Instance is released! Unrelease the instance before deletion.");
                }
            } else{
                throw new NotFoundException("Instance not found");
            }
        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).build();
        }
    }

}

