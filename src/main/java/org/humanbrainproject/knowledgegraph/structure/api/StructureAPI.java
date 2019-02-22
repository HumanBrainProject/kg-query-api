package org.humanbrainproject.knowledgegraph.structure.api;

import io.swagger.annotations.ApiOperation;
import org.humanbrainproject.knowledgegraph.annotations.ToBeTested;
import org.humanbrainproject.knowledgegraph.commons.authorization.control.AuthorizationContext;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusSchemaReference;
import org.humanbrainproject.knowledgegraph.query.entity.JsonDocument;
import org.humanbrainproject.knowledgegraph.structure.boundary.Structure;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpHeaders;
import org.springframework.scheduling.annotation.Scheduled;
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
    @Cacheable("structure")
    public JsonDocument getStructure(){
        authorizationContext.setMasterCredential();
        return structure.getStructure(false);
    }

    @ApiOperation(value="Flushes the cache of the structure request. Please note, that additionally, the cache is flushed every 24h.")
    @CacheEvict(allEntries = true, cacheNames = { "structure"})
    @Scheduled(fixedDelay = 86400000)
    @DeleteMapping("/cache")
    public void structureCacheEvict() {
        getStructure();
    }


    @GetMapping("/withLinks")
    @Cacheable("structureWithLinks")
    public JsonDocument getStructureWithLinks(){
        authorizationContext.setMasterCredential();
        return structure.getStructure(true);
    }



    @ApiOperation(value="Flushes the cache of the structure with links request. Please note, that additionally, the cache is flushed every 24h.")
    @CacheEvict(allEntries = true, cacheNames = { "structureWithLinks"})
    @Scheduled(fixedDelay = 86400000)
    @DeleteMapping("/withLinks/cache")
    public void structureWithLinksCacheEvict() {
        getStructureWithLinks();
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
