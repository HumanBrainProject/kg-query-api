package org.humanbrainproject.knowledgegraph.instances.api;

import io.swagger.annotations.Api;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusInstanceReference;
import org.humanbrainproject.knowledgegraph.instances.boundary.Instances;
import org.humanbrainproject.knowledgegraph.instances.entity.Client;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;

import javax.ws.rs.core.MediaType;
import java.util.Map;

@RestController
@RequestMapping(value = "/instances", produces = MediaType.APPLICATION_JSON)
@Api(value="/instances", description = "The API for managing instances")
public class InstancesAPI {

    @Autowired
    Instances instances;

    @GetMapping(value = "/{org}/{domain}/{schema}/{version}/{id}", consumes = { MediaType.WILDCARD})
    public ResponseEntity<Map> getInstance(@PathVariable("org") String org, @PathVariable("domain") String domain, @PathVariable("schema") String schema, @PathVariable("version") String version, @PathVariable("id") String id ) throws Exception{
        try{
            Map instance = instances.getInstance(new NexusInstanceReference(org, domain, schema, version, id));
            return instance!=null ? ResponseEntity.ok(instance) : ResponseEntity.notFound().build();
        } catch (HttpClientErrorException e){
            return ResponseEntity.status(e.getStatusCode()).build();
        }
    }

    @PutMapping(value = "/{org}/{domain}/{schema}/{version}/{id}", consumes = {MediaType.APPLICATION_JSON, "application/ld+json"})
    public ResponseEntity<Map> createOrUpdateInstance(@RequestBody String payload, @PathVariable("org") String org, @PathVariable("domain") String domain, @PathVariable("schema") String schema, @PathVariable("version") String version, @PathVariable("id") String id, @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationToken, @RequestHeader(value = "client", required = false, defaultValue = "none") Client client){
        return ResponseEntity.ok().build();
    }

    @DeleteMapping(value = "/{org}/{domain}/{schema}/{version}/{id}", consumes = {MediaType.APPLICATION_JSON, "application/ld+json"})
    public ResponseEntity<Void> deleteInstance(@RequestBody String payload, @PathVariable("org") String org, @PathVariable("domain") String domain, @PathVariable("schema") String schema, @PathVariable("version") String version, @PathVariable("id") String id, @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationToken, @RequestHeader(value = "client", required = false, defaultValue = "none") Client client){
        return ResponseEntity.ok().build();
    }



}

