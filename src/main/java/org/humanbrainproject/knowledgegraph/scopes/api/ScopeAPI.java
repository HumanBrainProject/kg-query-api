package org.humanbrainproject.knowledgegraph.scopes.api;

import io.swagger.annotations.Api;
import org.humanbrainproject.knowledgegraph.annotations.ToBeTested;
import org.humanbrainproject.knowledgegraph.commons.authorization.control.AuthorizationContext;
import org.humanbrainproject.knowledgegraph.commons.authorization.entity.InternalMasterKey;
import org.humanbrainproject.knowledgegraph.commons.authorization.entity.OidcAccessToken;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.builders.TreeScope;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.exceptions.StoredQueryNotFoundException;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusInstanceReference;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusSchemaReference;
import org.humanbrainproject.knowledgegraph.releasing.boundary.Releasing;
import org.humanbrainproject.knowledgegraph.releasing.entity.ReleaseStatusResponse;
import org.humanbrainproject.knowledgegraph.scopes.boundary.Scope;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.configurationprocessor.json.JSONException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;

import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.humanbrainproject.knowledgegraph.commons.api.ParameterConstants.*;

@RestController
@RequestMapping(value = "/api/scopes", produces = MediaType.APPLICATION_JSON)
@Api(value = "/api/scopes")
@ToBeTested(easy = true)
public class ScopeAPI {
    @Autowired
    Scope scope;

    @Autowired
    AuthorizationContext authorizationContext;

    @GetMapping(value = "/{"+ QUERY_ID+"}/{" + ORG + "}/{" + DOMAIN + "}/{" + SCHEMA + "}/{" + VERSION + "}/{" + ID + "}", consumes = {MediaType.WILDCARD})
    public ResponseEntity<Set<String>> getIdWhitelistForUser(@PathVariable(QUERY_ID) String queryId, @PathVariable(ORG) String org, @PathVariable(DOMAIN) String domain, @PathVariable(SCHEMA) String schema, @PathVariable(VERSION) String version, @PathVariable(ID) String id, @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        try {
            authorizationContext.setMasterCredential();
            Set<String> scope = this.scope.getIdWhitelistForUser(new NexusInstanceReference(new NexusSchemaReference(org, domain, schema, version), id), queryId, new OidcAccessToken().setToken(authorization));
            return ResponseEntity.ok(scope);
        } catch (StoredQueryNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).build();
        }
    }


    @GetMapping(value = "/{"+ QUERY_ID+"}", consumes = {MediaType.WILDCARD})
    public ResponseEntity<Set<String>> getIdWhitelistForUser(@PathVariable(QUERY_ID) String queryId, @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        try {
            authorizationContext.setMasterCredential();
            Set<String> scope = this.scope.getIdWhitelistForUser(queryId, new OidcAccessToken().setToken(authorization));
            return ResponseEntity.ok(scope);
        } catch (StoredQueryNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).build();
        }
    }








}
