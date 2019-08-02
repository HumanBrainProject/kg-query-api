package org.humanbrainproject.knowledgegraph.instances.api;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiParam;
import org.apache.solr.client.solrj.SolrServerException;
import org.humanbrainproject.knowledgegraph.annotations.ToBeTested;
import org.humanbrainproject.knowledgegraph.commons.authorization.control.AuthorizationContext;
import org.humanbrainproject.knowledgegraph.context.QueryContext;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusInstanceReference;
import org.humanbrainproject.knowledgegraph.instances.boundary.Instances;
import org.humanbrainproject.knowledgegraph.query.boundary.ArangoGraph;
import org.humanbrainproject.knowledgegraph.query.entity.ExposedDatabaseScope;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.configurationprocessor.json.JSONException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import springfox.documentation.annotations.ApiIgnore;

import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.humanbrainproject.knowledgegraph.commons.api.ParameterConstants.*;

@RestController
@RequestMapping(value = "/api/instances", produces = MediaType.APPLICATION_JSON)
@Api(value = "/api/instances", description = "The API for managing instances")
@ToBeTested(easy = true)
public class InstancesAPI {

    @Autowired
    Instances instances;

    @Autowired
    ArangoGraph graph;

    @Autowired
    AuthorizationContext authorizationContext;

    @Autowired
    QueryContext queryContext;

    @GetMapping(value = "/{org}/{domain}/{schema}/{version}/{id}/graph")
    public ResponseEntity<Map> getGraph(@PathVariable(ORG) String org, @PathVariable(DOMAIN) String domain, @PathVariable(SCHEMA) String schema, @PathVariable(VERSION) String version, @PathVariable("id") String id, @RequestParam(value= "step", required = false, defaultValue = "2") Integer step, @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationToken) {
        authorizationContext.populateAuthorizationContext(authorizationToken);
        try{
            NexusInstanceReference instanceReference = new NexusInstanceReference(org, domain, schema, version, id);
            //TODO Validate step value
            return ResponseEntity.ok(graph.getGraph(instanceReference, step));
        } catch (HttpClientErrorException e){
            return ResponseEntity.status(e.getStatusCode()).build();
        }
    }


    @PostMapping("/{queryId}")
    public ResponseEntity<List<Map>> getInstancesByIds(@RequestBody @ApiParam("The relative ids (starting with the organization) which shall be fetched") List<String> ids, @PathVariable("queryId") String queryId, @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationToken, @ApiParam(VOCAB_DOC)  @RequestParam(value = VOCAB, required = false) String vocab, @RequestParam(value = DATABASE_SCOPE, required = false) ExposedDatabaseScope databaseScope, @ApiIgnore @RequestParam Map<String, String> allRequestParams) throws SolrServerException, IOException, JSONException {
        authorizationContext.populateAuthorizationContext(authorizationToken);
        queryContext.populateQueryContext(databaseScope);
        Set<NexusInstanceReference> references = ids.stream().map(id -> NexusInstanceReference.createFromUrl(id)).collect(Collectors.toSet());
        List<Map> results = instances.getInstancesByReferences(references, queryId, vocab, allRequestParams);
        return ResponseEntity.ok(results);
    }


    @DeleteMapping(value = "/{org}/{domain}/{schema}/{version}/{id}")
    public ResponseEntity<Void> deleteInstance(@PathVariable(ORG) String org, @PathVariable(DOMAIN) String domain, @PathVariable(SCHEMA) String schema, @PathVariable(VERSION) String version, @PathVariable("id") String id, @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationToken) {
        authorizationContext.populateAuthorizationContext(authorizationToken);
        if (instances.removeInstance(new NexusInstanceReference(org, domain, schema, version, id))) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
}

