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
import org.apache.commons.configuration.ConversionException;
import org.humanbrainproject.knowledgegraph.annotations.ToBeTested;
import org.humanbrainproject.knowledgegraph.commons.InternalApi;
import org.humanbrainproject.knowledgegraph.commons.authorization.control.AuthorizationContext;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.entity.SubSpace;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusSchemaReference;
import org.humanbrainproject.knowledgegraph.instances.boundary.Schemas;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.ws.rs.core.MediaType;

import static org.humanbrainproject.knowledgegraph.commons.api.ParameterConstants.*;

@RestController
@RequestMapping(value = "/internal/api/schemas", produces = MediaType.APPLICATION_JSON)
@InternalApi
@Api(value = "/internal/api/schemas", description = "The API for managing schemas")
@ToBeTested(easy = true)
public class SchemasInternalAPI {

    @Autowired
    Schemas schemas;

    @Autowired
    AuthorizationContext authorizationContext;


    @DeleteMapping(value = "/{"+ORG+"}/{"+DOMAIN+"}/{"+SCHEMA+"}/{"+VERSION+"}/instances")
    public void clearAllInstancesInSchema(@PathVariable(ORG) String org, @PathVariable(DOMAIN) String domain, @PathVariable(SCHEMA) String schema, @PathVariable(VERSION) String version, @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationToken) {
        authorizationContext.populateAuthorizationContext(authorizationToken);
        schemas.clearAllInstancesOfSchema(new NexusSchemaReference(org, domain, schema, version));
    }

    @PutMapping(value = "/{"+ORG+"}/{"+DOMAIN+"}/{"+SCHEMA+"}/{"+VERSION+"}")
    public ResponseEntity<Void> createSimpleSchema(@PathVariable(ORG) String org, @PathVariable(DOMAIN) String domain, @PathVariable(SCHEMA) String schema, @PathVariable(VERSION) String version, @RequestParam(value = "subSpace", required = false) String subSpace, @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationToken) {
        try {
            authorizationContext.populateAuthorizationContext(authorizationToken);
            NexusSchemaReference schemaReference = new NexusSchemaReference(org, domain, schema, version);
            if (subSpace != null) {
                SubSpace space = SubSpace.byPostfix(subSpace);
                if (space != null) {
                    schemaReference = schemaReference.toSubSpace(space);
                } else {
                    throw new ConversionException("Could not convert subspace");
                }
            }
            schemas.createSimpleSchema(schemaReference);
            return ResponseEntity.ok().build();
        } catch (ConversionException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @PutMapping(value = "/{"+ORG+"}/{"+VERSION+"}")
    public ResponseEntity<Void> recreateSchemasInNewVersion(@PathVariable(ORG) String org, @PathVariable(VERSION) String version, @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationToken) {
        authorizationContext.populateAuthorizationContext(authorizationToken);
        schemas.createSchemasInNewVersion(org, version);
        return ResponseEntity.ok().build();
    }



}

