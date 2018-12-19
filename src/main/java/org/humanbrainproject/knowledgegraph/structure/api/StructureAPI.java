package org.humanbrainproject.knowledgegraph.structure.api;

import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusSchemaReference;
import org.humanbrainproject.knowledgegraph.query.entity.JsonDocument;
import org.humanbrainproject.knowledgegraph.structure.boundary.Structure;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.ws.rs.core.MediaType;
import java.util.List;

@RestController
@RequestMapping(value = "/api/structure", produces = MediaType.APPLICATION_JSON)
public class StructureAPI {

    @Autowired
    Structure structure;

    @GetMapping
    public JsonDocument getStructure(@RequestParam(value = "withLinks", required = false) boolean withLinks){
        return structure.getStructure(withLinks);
    }


    @GetMapping(value = "/{org}/{domain}/{schema}/{version}")
    public JsonDocument getStructureForSchema(@PathVariable("org") String org, @PathVariable("domain") String domain, @PathVariable("schema") String schema, @PathVariable("version") String version, @RequestParam(value = "withLinks", required = false, defaultValue = "false") boolean withLinks){
        return structure.getStructureForSchema(new NexusSchemaReference(org, domain, schema, version), withLinks);
    }

    @GetMapping("/{org}/{domain}/{schema}/{version}/bySpec")
    public void getStructureBySpecification(@PathVariable("org") String org, @PathVariable("domain") String domain, @PathVariable("schema") String schema, @PathVariable("version") String version){
        structure.reflectOnSpecifications(new NexusSchemaReference(org, domain, schema, version));
    }


    @GetMapping("/arango/edgeCollections")
    public List<String> getArangoEdgeCollections(){
        return structure.getArangoEdgeCollections();
    }

}
