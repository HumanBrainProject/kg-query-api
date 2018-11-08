package org.humanbrainproject.knowledgegraph.instances.api;

import io.swagger.annotations.Api;
import org.apache.commons.configuration.ConversionException;
import org.humanbrainproject.knowledgegraph.commons.authorization.entity.OidcAccessToken;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.entity.SubSpace;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusSchemaReference;
import org.humanbrainproject.knowledgegraph.instances.boundary.Schemas;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.ws.rs.core.MediaType;

@RestController
@RequestMapping(value = "/schemas", produces = MediaType.APPLICATION_JSON)
@Api(value="/schemas", description = "The API for managing schemas")
public class SchemasAPI {

    @Autowired
    Schemas schemas;

    @DeleteMapping(value = "/{org}/{domain}/{schema}/{version}/instances")
    public void clearAllInstancesInSchema(@PathVariable("org") String org, @PathVariable("domain") String domain, @PathVariable("schema") String schema, @PathVariable("version") String version, @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationToken){
        schemas.clearAllInstancesOfSchema(new NexusSchemaReference(org, domain, schema, version), new OidcAccessToken().setToken(authorizationToken));
    }


    @PutMapping(value = "/{org}/{domain}/{schema}/{version}", consumes = { MediaType.APPLICATION_JSON})
    public ResponseEntity<Void> createSimpleSchema(@PathVariable("org") String org, @PathVariable("domain") String domain, @PathVariable("schema") String schema, @PathVariable("version") String version, @RequestParam(value = "subSpace", required = false) String subSpace ){
        try{

            NexusSchemaReference schemaReference = new NexusSchemaReference(org, domain, schema, version);
            if(subSpace!=null){
                SubSpace space = SubSpace.byPostfix(subSpace);
                if(space != null){
                    schemaReference = schemaReference.toSubSpace(space);
                }else{
                    throw new ConversionException("Could not convert subspace");
                }
            }
            schemas.createSimpleSchema(schemaReference);
            return ResponseEntity.ok().build();
        } catch (ConversionException e){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }


}

