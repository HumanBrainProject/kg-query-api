package org.humanbrainproject.knowledgegraph.releasing.api;

import io.swagger.annotations.Api;
import org.humanbrainproject.knowledgegraph.commons.authorization.entity.OidcAccessToken;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusInstanceReference;
import org.humanbrainproject.knowledgegraph.releasing.boundary.Releasing;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.ws.rs.core.MediaType;

@RestController
@RequestMapping(value = "/releasing", produces = MediaType.APPLICATION_JSON)
@Api(value = "/releasing", description = "The extension API to release resources in the Knowledge Graph")
public class ReleasingAPI {

    @Autowired
    Releasing releasing;


    @PutMapping(value = "/schemas/{org}/{domain}/{schema}/{version}/{id}", consumes = {MediaType.APPLICATION_JSON})
    public ResponseEntity<Void> release(@PathVariable("org") String org, @PathVariable("domain") String domain, @PathVariable("schema") String schema, @PathVariable("version") String version, @RequestParam(value = "id") String id, @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationToken) {
        NexusInstanceReference nexusInstanceReference = new NexusInstanceReference(org, domain, schema, version, id);
        releasing.release(nexusInstanceReference, new OidcAccessToken().setToken(authorizationToken));
        return ResponseEntity.ok().build();

    }


}
