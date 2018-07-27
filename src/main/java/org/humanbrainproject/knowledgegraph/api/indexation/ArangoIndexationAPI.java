package org.humanbrainproject.knowledgegraph.api.indexation;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.humanbrainproject.knowledgegraph.boundary.indexation.ArangoIndexation;
import org.humanbrainproject.knowledgegraph.control.VertexRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.configurationprocessor.json.JSONException;
import org.springframework.web.bind.annotation.*;

import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.util.logging.Logger;

@RestController
@RequestMapping(value = "/arango")
@Api(value="/arango", description = "The indexation api to upload JSON-LD to the arango database")
public class ArangoIndexationAPI {

    @Autowired
    ArangoIndexation indexer;

    @ApiOperation("Creates a new instance")
    @PostMapping(value="/{organization}/{domain}/{schema}/{schemaversion}/{id}", consumes = {MediaType.APPLICATION_JSON, "application/ld+json"}, produces = MediaType.APPLICATION_JSON)
    public void addInstance(@RequestBody String payload, @PathVariable("organization") String organization, @PathVariable("domain") String domain, @PathVariable("schema") String schema, @PathVariable("schemaversion") String schemaVersion, @PathVariable("id") String id) throws IOException, JSONException {
        indexer.insertJsonOrJsonLd(buildEntityName(organization, domain, schema, schemaVersion), id, payload, buildDefaultNamespace(organization, domain, schema, schemaVersion));
    }

    @PutMapping(value="/{organization}/{domain}/{schema}/{schemaversion}/{id}/{rev}", consumes = {MediaType.APPLICATION_JSON, "application/ld+json"}, produces = MediaType.APPLICATION_JSON)
    public void updateInstance(@RequestBody String payload, @PathVariable("organization") String organization, @PathVariable("domain") String domain, @PathVariable("schema") String schema, @PathVariable("schemaversion") String schemaVersion, @PathVariable("id") String id, @PathVariable("rev") Integer rev, @QueryParam("defaultNamespace") String defaultNamespace, @QueryParam("vertexLabel") String vertexLabel) throws IOException, JSONException {
        indexer.updateJsonOrJsonLd(buildEntityName(organization, domain, schema, schemaVersion), id, rev, payload, buildDefaultNamespace(organization, domain, schema, schemaVersion));
    }

    @DeleteMapping(value="/{organization}/{domain}/{schema}/{schemaversion}/{id}", produces = MediaType.APPLICATION_JSON)
    public void deleteInstance(@PathVariable("organization") String organization, @PathVariable("domain") String domain, @PathVariable("schema") String schema, @PathVariable("schemaversion") String schemaVersion, @PathVariable("id") String id, @RequestAttribute(value="rev", required = false) Integer rev) throws IOException, JSONException {
        indexer.delete(buildEntityName(organization, domain, schema, schemaVersion), id, rev);
    }

    private String buildEntityName(String organization, String domain, String schema, String schemaVersion){
        return String.format("%s/%s/%s/%s", organization, domain, schema, schemaVersion);
    }

    private String buildDefaultNamespace(String organization, String domain, String schema, String schemaVersion){
        return String.format("http://schema.hbp.eu/%s/%s/%s/%s#", organization, domain, schema, schemaVersion);
    }

    @DeleteMapping("/collections")
    public void clearGraph(){
        indexer.clearGraph();
    }

}
