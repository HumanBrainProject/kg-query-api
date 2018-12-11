package org.humanbrainproject.knowledgegraph.instances.api;

import io.swagger.annotations.Api;
import org.humanbrainproject.knowledgegraph.commons.authorization.entity.OidcAccessToken;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusInstanceReference;
import org.humanbrainproject.knowledgegraph.instances.boundary.Instances;
import org.humanbrainproject.knowledgegraph.query.boundary.ArangoGraph;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;

import javax.ws.rs.core.MediaType;
import java.util.Map;

import static org.humanbrainproject.knowledgegraph.commons.api.ParameterConstants.*;

@RestController
@RequestMapping(value = "/api/instances", produces = MediaType.APPLICATION_JSON)
@Api(value = "/api/instances", description = "The API for managing instances")
public class InstancesAPI {

    @Autowired
    Instances instances;

    @Autowired
    ArangoGraph graph;


    @GetMapping(value = "/{org}/{domain}/{schema}/{version}/{id}/graph")
    public ResponseEntity<Map> getGraph(@PathVariable("org") String org, @PathVariable("domain") String domain, @PathVariable("schema") String schema, @PathVariable("version") String version, @PathVariable("id") String id, @RequestParam(value= "step", required = false, defaultValue = "2") Integer step, @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationToken) throws Exception{
        try{
            NexusInstanceReference instanceReference = new NexusInstanceReference(org, domain, schema, version, id);
            //TODO Validate step value
            return ResponseEntity.ok(graph.getGraph(instanceReference, step, new OidcAccessToken().setToken(authorizationToken)));
        } catch (HttpClientErrorException e){
            return ResponseEntity.status(e.getStatusCode()).build();
        }
    }


    @DeleteMapping(value = "/{org}/{domain}/{schema}/{version}/{id}")
    public ResponseEntity<Void> deleteInstance(@PathVariable(ORG) String org, @PathVariable(DOMAIN) String domain, @PathVariable(SCHEMA) String schema, @PathVariable(VERSION) String version, @PathVariable("id") String id, @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationToken) {
        if (instances.removeInstance(new NexusInstanceReference(org, domain, schema, version, id), new OidcAccessToken().setToken(authorizationToken))) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
}

