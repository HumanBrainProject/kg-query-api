/*
 * Copyright 2018 - 2021 Swiss Federal Institute of Technology Lausanne (EPFL)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * This open source software code was developed in part or in whole in the
 * Human Brain Project, funded from the European Union's Horizon 2020
 * Framework Programme for Research and Innovation under
 * Specific Grant Agreements No. 720270, No. 785907, and No. 945539
 * (Human Brain Project SGA1, SGA2 and SGA3).
 *
 */

package org.humanbrainproject.knowledgegraph.releasing.api;

import io.swagger.annotations.Api;
import org.humanbrainproject.knowledgegraph.annotations.ToBeTested;
import org.humanbrainproject.knowledgegraph.commons.authorization.control.AuthorizationContext;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.builders.TreeScope;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.exceptions.StoredQueryNotFoundException;
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

import static org.humanbrainproject.knowledgegraph.commons.api.ParameterConstants.*;

@RestController
@RequestMapping(value = "/api/releases", produces = MediaType.APPLICATION_JSON)
@Api(value = "/api/releases", description = "The extension API to release resources in the Knowledge Graph")
@ToBeTested(easy = true)
public class ReleasingAPI {

    @Autowired
    AuthorizationContext authorizationContext;

    @Autowired
    Releasing releasing;

    @GetMapping(value = "/{" + ORG + "}/{" + DOMAIN + "}/{" + SCHEMA + "}/{" + VERSION + "}/{" + ID + "}", consumes = {MediaType.WILDCARD})
    public ResponseEntity<ReleaseStatusResponse> getReleaseStatus(@PathVariable(ORG) String org, @PathVariable(DOMAIN) String domain, @PathVariable(SCHEMA) String schema, @PathVariable(VERSION) String version, @PathVariable(ID) String id, @RequestParam(value = "releaseTreeScope", defaultValue = "ALL") TreeScope scope, @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        try {
            authorizationContext.populateAuthorizationContext(authorization);
            ReleaseStatusResponse releaseStatus = releasing.getReleaseStatus(new NexusInstanceReference(org, domain, schema, version, id), scope);
            if (releaseStatus == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(releaseStatus);
        } catch (StoredQueryNotFoundException e){
            return ResponseEntity.notFound().build();
        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).build();
        }
    }


    @PostMapping(consumes = {MediaType.APPLICATION_JSON})
    public ResponseEntity<List<ReleaseStatusResponse>> getReleaseStatusList(@RequestBody List<String> relativeNexusIds, @RequestParam(value = "releaseTreeScope", defaultValue = "ALL") TreeScope scope, @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        try {
            authorizationContext.populateAuthorizationContext(authorization);
            if (relativeNexusIds != null) {
                List<ReleaseStatusResponse> collect = relativeNexusIds.stream().map(ref -> releasing.getReleaseStatus(NexusInstanceReference.createFromUrl(ref), scope)).collect(Collectors.toList());
                return ResponseEntity.ok(collect);
            }
            return ResponseEntity.badRequest().build();
        } catch (StoredQueryNotFoundException e){
            return ResponseEntity.notFound().build();
        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).build();
        }
    }


    @GetMapping(value = "/{" + ORG + "}/{" + DOMAIN + "}/{" + SCHEMA + "}/{" + VERSION + "}/{" + ID + "}/graph", consumes = {MediaType.WILDCARD})
    public ResponseEntity<Map<String, Object>> getReleaseGraph(@PathVariable(ORG) String org, @PathVariable(DOMAIN) String domain, @PathVariable(SCHEMA) String schema, @PathVariable(VERSION) String version, @PathVariable(ID) String id, @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationToken) {
        try {
            authorizationContext.populateAuthorizationContext(authorizationToken);
            Map<String, Object> releaseGraph = releasing.getReleaseGraph(new NexusInstanceReference(org, domain, schema, version, id));
            if (releaseGraph == null) {
                return ResponseEntity.notFound().build();
            } else {
                return ResponseEntity.ok(releaseGraph);
            }
        } catch (StoredQueryNotFoundException e){
            return ResponseEntity.notFound().build();
        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).build();
        }
    }


    @PutMapping(value = "/{" + ORG + "}/{" + DOMAIN + "}/{" + SCHEMA + "}/{" + VERSION + "}/{" + ID + "}")
    public ResponseEntity<Void> release(@PathVariable(ORG) String org, @PathVariable(DOMAIN) String domain, @PathVariable(SCHEMA) String schema, @PathVariable(VERSION) String version, @PathVariable(ID) String id, @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationToken) {
        authorizationContext.populateAuthorizationContext(authorizationToken);
        releasing.release(new NexusInstanceReference(org, domain, schema, version, id));
        return ResponseEntity.ok().build();
    }

    @DeleteMapping(value = "/{" + ORG + "}/{" + DOMAIN + "}/{" + SCHEMA + "}/{" + VERSION + "}/{" + ID + "}")
    public ResponseEntity<Void> unrelease(@PathVariable(ORG) String org, @PathVariable(DOMAIN) String domain, @PathVariable(SCHEMA) String schema, @PathVariable(VERSION) String version, @PathVariable(ID) String id, @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationToken) {
        authorizationContext.populateAuthorizationContext(authorizationToken);
        NexusInstanceReference unreleasedInstance = releasing.unrelease( new NexusInstanceReference(org, domain, schema, version, id));
        if (unreleasedInstance != null) {
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }

}
