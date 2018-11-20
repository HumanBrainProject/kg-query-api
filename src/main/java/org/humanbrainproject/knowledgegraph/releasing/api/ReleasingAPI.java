package org.humanbrainproject.knowledgegraph.releasing.api;

import io.swagger.annotations.Api;
import org.humanbrainproject.knowledgegraph.commons.authorization.entity.OidcAccessToken;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusInstanceReference;
import org.humanbrainproject.knowledgegraph.releasing.boundary.Releasing;
import org.humanbrainproject.knowledgegraph.releasing.entity.ReleaseStatusResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;

import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping(value = "/api/releases", produces = MediaType.APPLICATION_JSON)
@Api(value = "/api/releases", description = "The extension API to release resources in the Knowledge Graph")
public class ReleasingAPI {

    @Autowired
    Releasing releasing;

    @GetMapping(value = "/{org}/{domain}/{schema}/{version}/{id}", consumes = { MediaType.WILDCARD})
    public ResponseEntity<ReleaseStatusResponse> getReleaseStatus(@PathVariable("org") String org, @PathVariable("domain") String domain, @PathVariable("schema") String schema, @PathVariable("version") String version, @PathVariable("id") String id) {
        try{
            NexusInstanceReference instanceReference = new NexusInstanceReference(org, domain, schema, version, id);
            ReleaseStatusResponse releaseStatus = releasing.getReleaseStatus(instanceReference);
            if(releaseStatus==null){
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(releaseStatus);
        } catch (HttpClientErrorException e){
            return ResponseEntity.status(e.getStatusCode()).build();
        }
    }


    @PostMapping(consumes = { MediaType.APPLICATION_JSON})
    public ResponseEntity<List<ReleaseStatusResponse>> getReleaseStatusList(@RequestBody List<String> relativeNexusIds) {
        try{
            if(relativeNexusIds!=null){
                List<ReleaseStatusResponse> collect = relativeNexusIds.stream().map(ref -> releasing.getReleaseStatus(NexusInstanceReference.createFromUrl(ref))).collect(Collectors.toList());
                return ResponseEntity.ok(collect);
            }
            return ResponseEntity.badRequest().build();
        } catch (HttpClientErrorException e){
            return ResponseEntity.status(e.getStatusCode()).build();
        }
    }



    @GetMapping(value = "/{org}/{domain}/{schema}/{version}/{id}/graph", consumes = { MediaType.WILDCARD})
    public ResponseEntity<Map<String,Object>> getReleaseGraph(@PathVariable("org") String org, @PathVariable("domain") String domain, @PathVariable("schema") String schema, @PathVariable("version") String version, @PathVariable("id") String id, @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationToken) {
        try{
            NexusInstanceReference instanceReference = new NexusInstanceReference(org, domain, schema, version, id);
            Map<String, Object> releaseGraph = releasing.getReleaseGraph(instanceReference);
            if(releaseGraph==null){
                return ResponseEntity.notFound().build();
            }
            else{
                return ResponseEntity.ok(releaseGraph);
            }
        } catch (HttpClientErrorException e){
            return ResponseEntity.status(e.getStatusCode()).build();
        }
    }



    @PutMapping(value = "/{org}/{domain}/{schema}/{version}/{id}")
    public ResponseEntity<Void> release(@PathVariable("org") String org, @PathVariable("domain") String domain, @PathVariable("schema") String schema, @PathVariable("version") String version, @PathVariable("id") String id, @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationToken) {
        NexusInstanceReference nexusInstanceReference = new NexusInstanceReference(org, domain, schema, version, id);
        releasing.release(nexusInstanceReference, new OidcAccessToken().setToken(authorizationToken));
        return ResponseEntity.ok().build();
    }

    @DeleteMapping(value = "/{org}/{domain}/{schema}/{version}/{id}")
    public ResponseEntity<Void> unrelease(@PathVariable("org") String org, @PathVariable("domain") String domain, @PathVariable("schema") String schema, @PathVariable("version") String version, @PathVariable("id") String id, @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationToken) {
        NexusInstanceReference nexusInstanceReference = new NexusInstanceReference(org, domain, schema, version, id);
        releasing.unrelease(nexusInstanceReference, new OidcAccessToken().setToken(authorizationToken));
        return ResponseEntity.ok().build();
    }

}
