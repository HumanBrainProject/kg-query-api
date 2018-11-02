package org.humanbrainproject.knowledgegraph.nexusExt.api;

import io.swagger.annotations.Api;
import org.apache.commons.configuration.ConversionException;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.entity.SubSpace;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusSchemaReference;
import org.humanbrainproject.knowledgegraph.nexusExt.boundary.NexusExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.ws.rs.core.MediaType;
import java.io.IOException;

@RestController
@RequestMapping(value = "/nexus", produces = MediaType.APPLICATION_JSON)
@Api(value="/nexus", description = "The extension API for managing resources on Nexus")
public class NexusExtensionAPI {

    @Autowired
    NexusExtension nexusExtension;



    @PutMapping(value = "/schemas/{org}/{domain}/{schema}/{version}", consumes = { MediaType.APPLICATION_JSON})
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
            nexusExtension.createSimpleSchema(schemaReference);
            return ResponseEntity.ok().build();
        } catch (IOException e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } catch (ConversionException e){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }


}

