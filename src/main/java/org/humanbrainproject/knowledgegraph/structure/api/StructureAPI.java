package org.humanbrainproject.knowledgegraph.structure.api;

import org.humanbrainproject.knowledgegraph.annotations.ToBeTested;
import org.humanbrainproject.knowledgegraph.commons.authorization.control.AuthorizationContext;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusSchemaReference;
import org.humanbrainproject.knowledgegraph.query.entity.JsonDocument;
import org.humanbrainproject.knowledgegraph.structure.boundary.Structure;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

import javax.ws.rs.core.MediaType;
import java.util.List;

import static org.humanbrainproject.knowledgegraph.commons.api.ParameterConstants.*;

@RestController
@RequestMapping(value = "/api/structure", produces = MediaType.APPLICATION_JSON)
@ToBeTested(easy = true)
public class StructureAPI {

    @Autowired
    AuthorizationContext authorizationContext;

    @Autowired
    Structure structure;

    @GetMapping
    public JsonDocument getStructure(@RequestParam(value = "withLinks", required = false) boolean withLinks, @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization){
        authorizationContext.populateAuthorizationContext(authorization);
        return structure.getStructure(withLinks);
    }


    @GetMapping(value = "/{"+ORG+"}/{"+ DOMAIN+"}/{"+SCHEMA+"}/{"+VERSION+"}")
    public JsonDocument getStructureForSchema(@PathVariable(ORG) String org, @PathVariable(DOMAIN) String domain, @PathVariable(SCHEMA) String schema, @PathVariable(VERSION) String version, @RequestParam(value = "withLinks", required = false, defaultValue = "false") boolean withLinks, @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization){
        authorizationContext.populateAuthorizationContext(authorization);
        return structure.getStructureForSchema(new NexusSchemaReference(org, domain, schema, version), withLinks);
    }

    @GetMapping("/{"+ORG+"}/{"+ DOMAIN+"}/{"+SCHEMA+"}/{"+VERSION+"}/bySpec")
    public void getStructureBySpecification(@PathVariable(ORG) String org, @PathVariable(DOMAIN) String domain, @PathVariable(SCHEMA) String schema, @PathVariable(VERSION) String version, @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization){
        authorizationContext.populateAuthorizationContext(authorization);
        structure.reflectOnSpecifications(new NexusSchemaReference(org, domain, schema, version));
    }


    @GetMapping("/arango/edgeCollections")
    public List<String> getArangoEdgeCollections(){
        return structure.getArangoEdgeCollections();
    }

}
