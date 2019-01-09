package org.humanbrainproject.knowledgegraph.query.api;

import io.swagger.annotations.Api;
import org.humanbrainproject.knowledgegraph.annotations.ToBeTested;
import org.humanbrainproject.knowledgegraph.commons.api.RestUtils;
import org.humanbrainproject.knowledgegraph.commons.jsonld.control.JsonTransformer;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.ArangoToNexusLookupMap;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.exceptions.RootCollectionNotFoundException;
import org.humanbrainproject.knowledgegraph.commons.authorization.control.AuthorizationContext;
import org.humanbrainproject.knowledgegraph.context.QueryContext;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusSchemaReference;
import org.humanbrainproject.knowledgegraph.query.boundary.Suggestions;
import org.humanbrainproject.knowledgegraph.query.entity.SuggestionResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;

import javax.ws.rs.core.MediaType;
import java.util.List;

import static org.humanbrainproject.knowledgegraph.commons.api.ParameterConstants.*;

@RestController
@RequestMapping(value = "/suggestions", produces = MediaType.APPLICATION_JSON)
@Api(value = "/suggestions", description = "The API for suggestions")
@ToBeTested(easy = true)
public class SuggestionAPI {


    @Autowired
    AuthorizationContext authorizationContext;

    @Autowired
    QueryContext queryContext;

    @Autowired
    Suggestions suggestions;

    @Autowired
    JsonTransformer jsonTransformer;

    @Autowired
    ArangoToNexusLookupMap lookupMap;


    @PostMapping(value = "/fields/{"+ORG+"}/{"+ DOMAIN+"}/{"+SCHEMA+"}/{"+VERSION+"}", consumes = {MediaType.APPLICATION_JSON, RestUtils.APPLICATION_LD_JSON})
    public ResponseEntity<List<SuggestionResult>> suggestionForInstanceByField(@PathVariable(ORG) String org, @PathVariable(DOMAIN) String domain, @PathVariable(SCHEMA) String schema, @PathVariable(VERSION) String version, @RequestBody(required = false) String payload, @RequestParam(value = "field") String field, @RequestParam(value = "q", required = false) String query, @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) throws Exception {
        try {
            authorizationContext.populateAuthorizationContext(authorization);

            List<SuggestionResult> suggestionResults = suggestions.suggestionForInstanceByField(new NexusSchemaReference(org, domain, schema, version), field);
            if(suggestionResults!=null){
                return ResponseEntity.ok(suggestionResults);
            }
            return ResponseEntity.notFound().build();
        } catch (RootCollectionNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).build();
        }
    }

    @PostMapping(value = "/instances", consumes = {MediaType.APPLICATION_JSON, RestUtils.APPLICATION_LD_JSON})
    public ResponseEntity<List<SuggestionResult>> suggestionForInstanceByType(@RequestBody String payload, @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) throws Exception {
        try {
            authorizationContext.populateAuthorizationContext(authorization);

            List<String> ids = jsonTransformer.parseToListOfStrings(payload);
            List<SuggestionResult> suggestionResults = suggestions.suggestionForInstanceById(ids);
            if(suggestionResults!=null){
                return ResponseEntity.ok(suggestionResults);
            }
            return ResponseEntity.notFound().build();
        } catch (RootCollectionNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).build();
        }
    }
}
