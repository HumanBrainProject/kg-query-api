package org.humanbrainproject.knowledgegraph.suggestion.api;

import org.humanbrainproject.knowledgegraph.annotations.ToBeTested;
import org.humanbrainproject.knowledgegraph.commons.api.Client;
import org.humanbrainproject.knowledgegraph.commons.api.RestUtils;
import org.humanbrainproject.knowledgegraph.commons.authorization.control.AuthorizationContext;
import org.humanbrainproject.knowledgegraph.commons.authorization.control.SystemOidcClient;
import org.humanbrainproject.knowledgegraph.commons.authorization.entity.OidcAccessToken;
import org.humanbrainproject.knowledgegraph.commons.suggestion.SuggestionStatus;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusInstanceReference;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusSchemaReference;
import org.humanbrainproject.knowledgegraph.query.entity.JsonDocument;
import org.humanbrainproject.knowledgegraph.query.entity.Pagination;
import org.humanbrainproject.knowledgegraph.query.entity.QueryResult;
import org.humanbrainproject.knowledgegraph.suggestion.boundary.Suggest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.Map;

import static org.humanbrainproject.knowledgegraph.commons.api.ParameterConstants.*;

@RestController
@RequestMapping(value = "/api/suggestion", produces = MediaType.APPLICATION_JSON)
@ToBeTested(easy = true)
public class SuggestionAPI {

    @Autowired
    AuthorizationContext authorizationContext;

    @Autowired
    Suggest suggest;

    @Autowired
    SystemOidcClient OIDCclient;


    private Logger logger = LoggerFactory.getLogger(SuggestionAPI.class);


    @PostMapping(value="/{"+ ORG+"}/{"+DOMAIN+"}/{"+SCHEMA+"}/{"+VERSION+"}/fields", consumes = {MediaType.APPLICATION_JSON, RestUtils.APPLICATION_LD_JSON}, produces = MediaType.APPLICATION_JSON)
    public ResponseEntity<QueryResult<List<Map>>> getStructureForSchemaByField(@RequestBody(required = false) String payload, @PathVariable(ORG) String org, @PathVariable(DOMAIN) String domain, @PathVariable(SCHEMA) String schema, @PathVariable(VERSION) String version, @RequestParam(value = "field") String field, @RequestParam(value = SEARCH, required = false) String search, @RequestParam(value = SIZE, required = false) Integer size, @RequestParam(value = START, required = false) Integer start, @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization){
        authorizationContext.populateAuthorizationContext(authorization);

        NexusSchemaReference schemaReference = new NexusSchemaReference(org, domain, schema, version);

        Pagination pagination = new Pagination();
        pagination.setStart(start==null ? 0 : start);
        pagination.setSize(size);
        logger.info(String.format("Loading suggestion for object %s and field %s", schemaReference, field));
        logger.info(String.format("Payload: %s", payload));
        return ResponseEntity.ok(suggest.suggestByField(schemaReference, field, search, pagination));
    }

    @PostMapping(value="/{"+ ORG+"}/{"+DOMAIN+"}/{"+SCHEMA+"}/{"+VERSION+"}/{"+ INSTANCE_ID +"}/instance/{userId}", consumes = {MediaType.APPLICATION_JSON, RestUtils.APPLICATION_LD_JSON, MediaType.WILDCARD}, produces = MediaType.APPLICATION_JSON)
    public ResponseEntity<Map> createSuggestionInstanceForUser(@RequestBody(required = false) String payload, @PathVariable(ORG) String org, @PathVariable(DOMAIN) String domain, @PathVariable(SCHEMA) String schema, @PathVariable(VERSION) String version, @PathVariable(INSTANCE_ID) String instanceId, @PathVariable("userId") String userId, @RequestParam(value = CLIENT_ID_EXTENSION, required = true) String clientIdExtension, @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization, @RequestHeader(value = CLIENT, required = false) Client client) throws HttpClientErrorException{
        authorizationContext.populateAuthorizationContext(authorization, client);
        try{
            NexusInstanceReference instanceReference = new NexusInstanceReference(org, domain, schema, version, instanceId);
            Map instance = suggest.getUserSuggestionOfSpecificInstance(instanceReference, userId);
            if(instance == null){
                NexusInstanceReference created = suggest.createSuggestionInstanceForUser(instanceReference, userId, clientIdExtension);
                if(created != null){
                    return ResponseEntity.ok().build();
                }else{
                    throw new InternalServerErrorException("Could not created instance");
                }
            }else{
                throw new BadRequestException("User already added to this instance");
            }
        }catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).build();
        }
    }

    @GetMapping(value="/user", consumes = {MediaType.APPLICATION_JSON, RestUtils.APPLICATION_LD_JSON, MediaType.WILDCARD}, produces = MediaType.APPLICATION_JSON)
    public ResponseEntity<List<Map>> getSuggestionOfUser(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization, @RequestHeader(value = CLIENT, required = false) Client client, @RequestParam(value = "status") SuggestionStatus status) throws Exception{
        authorizationContext.populateAuthorizationContext(authorization, client);
        Map user = OIDCclient.getUserInfo(new OidcAccessToken().setToken(authorization));
        String userId = (String) user.get("sub");
        List<Map> instances = suggest.getUserSuggestions(userId, status);
        if(instances != null){
            return ResponseEntity.ok(instances);
        }else {
            throw new NotFoundException("Suggestion not found");
        }
    }

    @PostMapping(value="/{"+ ORG+"}/{"+DOMAIN+"}/{"+SCHEMA+"}/{"+VERSION+"}/{"+ INSTANCE_ID +"}/accept", consumes = {MediaType.APPLICATION_JSON, RestUtils.APPLICATION_LD_JSON, MediaType.WILDCARD}, produces = MediaType.APPLICATION_JSON)
    public ResponseEntity<List<Map>> acceptSuggestion(@PathVariable(ORG) String org, @PathVariable(DOMAIN) String domain, @PathVariable(SCHEMA) String schema, @PathVariable(VERSION) String version, @PathVariable(INSTANCE_ID) String instanceId, @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization, @RequestHeader(value = CLIENT, required = false) Client client) throws HttpClientErrorException{
        authorizationContext.populateAuthorizationContext(authorization, client);
        Map user = OIDCclient.getUserInfo(new OidcAccessToken().setToken(authorization));
        String userId = (String) user.get("sub");
        NexusInstanceReference suggestionInstanceRef = new NexusInstanceReference(org, domain, schema, version, instanceId);
        try {

           JsonDocument m = suggest.changeSuggestionStatus(suggestionInstanceRef, SuggestionStatus.ACCEPTED, userId);
           if(m != null){
               return ResponseEntity.ok().build();
           }else{
               throw new InternalServerErrorException("Could not accept suggestion");
           }
        }catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).build();
        }
    }

    @PostMapping(value="/{"+ ORG+"}/{"+DOMAIN+"}/{"+SCHEMA+"}/{"+VERSION+"}/{"+ INSTANCE_ID +"}/reject", consumes = {MediaType.APPLICATION_JSON, RestUtils.APPLICATION_LD_JSON, MediaType.WILDCARD}, produces = MediaType.APPLICATION_JSON)
    public ResponseEntity<List<Map>> rejectSuggestion(@PathVariable(ORG) String org, @PathVariable(DOMAIN) String domain, @PathVariable(SCHEMA) String schema, @PathVariable(VERSION) String version, @PathVariable(INSTANCE_ID) String instanceId, @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization, @RequestHeader(value = CLIENT, required = false) Client client) throws HttpClientErrorException{
        authorizationContext.populateAuthorizationContext(authorization, client);
        Map user = OIDCclient.getUserInfo(new OidcAccessToken().setToken(authorization));
        String userId = (String) user.get("sub");
        NexusInstanceReference suggestionInstanceRef = new NexusInstanceReference(org, domain, schema, version, instanceId);
        try {
            JsonDocument m = suggest.changeSuggestionStatus(suggestionInstanceRef, SuggestionStatus.REJECTED, userId);
            if(m != null){
                return ResponseEntity.ok().build();
            }else{
                throw new InternalServerErrorException("Could not accept suggestion");
            }
        }catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).build();
        }
    }




}
