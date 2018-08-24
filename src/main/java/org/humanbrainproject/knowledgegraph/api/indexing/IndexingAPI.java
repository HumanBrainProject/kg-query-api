package org.humanbrainproject.knowledgegraph.api.indexing;

import com.github.jsonldjava.core.JsonLdError;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.humanbrainproject.knowledgegraph.boundary.indexing.ArangoIndexing;
import org.humanbrainproject.knowledgegraph.boundary.indexing.GraphIndexing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.configurationprocessor.json.JSONException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.ws.rs.core.MediaType;
import java.io.IOException;

@RestController
@RequestMapping(value = "/indexing")
@Api(value="/indexing", description = "The indexing api to upload JSON-LD to the arango database")
public class IndexingAPI {

    @Autowired
    ArangoIndexing indexer;

    Logger logger = LoggerFactory.getLogger(IndexingAPI.class);


    @GetMapping(value="/{organization}/{domain}/{schema}/{schemaversion}/{id}", produces = MediaType.APPLICATION_JSON)
    public ResponseEntity<String> fetchInstance(@PathVariable("organization") String organization, @PathVariable("domain") String domain, @PathVariable("schema") String schema, @PathVariable("schemaversion") String schemaVersion, @PathVariable("id") String id) {
        String entityName = buildEntityName(organization, domain, schema, schemaVersion);
        logger.info(String.format("Received get request for {}/{}", entityName, id));
        try {
            return ResponseEntity.ok(indexer.getById(entityName, id));
        } catch (JsonLdError e) {
            logger.warn("Was not able to process the payload", e);
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch(Exception e){
            logger.error(String.format("GET: Was not able to fetch the instance %s", entityName), e);
            throw new RuntimeException(e);
        }
    }

    @ApiOperation("Creates a new instance")
    @PostMapping(value="/{organization}/{domain}/{schema}/{schemaversion}/{id}", consumes = {MediaType.APPLICATION_JSON, "application/ld+json"}, produces = MediaType.APPLICATION_JSON)
    public ResponseEntity<String> addInstance(@RequestBody String payload, @PathVariable("organization") String organization, @PathVariable("domain") String domain, @PathVariable("schema") String schema, @PathVariable("schemaversion") String schemaVersion, @PathVariable("id") String id, @RequestParam(value = "authorId", required = false) String authorId, @RequestParam(value = "timestamp", required = false) String timestamp) {
        String entityName = buildEntityName(organization, domain, schema, schemaVersion);
        logger.info("Received insert request for {}/{}", entityName, id);
        logger.debug("Payload for insert request {}/{}: {}", entityName, id, payload);
        try {
            GraphIndexing.GraphIndexationSpec spec = new GraphIndexing.GraphIndexationSpec();
            spec.setJsonOrJsonLdPayload(payload).setPermissionGroup(organization).setEntityName(entityName).setId(id).setDefaultNamespace(buildDefaultNamespace(organization, domain, schema, schemaVersion));
            indexer.insertJsonOrJsonLd(spec);
            return ResponseEntity.ok(null);
        } catch (JSONException | JsonLdError e) {
            logger.warn(String.format("INS: Was not able to process the payload %s", payload), e);
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch(Exception e){
            logger.error(String.format("INS: Was not able to insert the instance %s with the payload %s", entityName, payload), e);
            throw new RuntimeException(e);
        }
    }

    @PutMapping(value="/{organization}/{domain}/{schema}/{schemaversion}/{id}/{rev}", consumes = {MediaType.APPLICATION_JSON, "application/ld+json"}, produces = MediaType.APPLICATION_JSON)
    public ResponseEntity<String> updateInstance(@RequestBody String payload, @PathVariable("organization") String organization, @PathVariable("domain") String domain, @PathVariable("schema") String schema, @PathVariable("schemaversion") String schemaVersion, @PathVariable("id") String id, @PathVariable("rev") Integer rev, @RequestParam(value = "authorId", required = false) String authorId, @RequestParam(value = "timestamp", required = false) String timestamp) {
        String entityName = buildEntityName(organization, domain, schema, schemaVersion);
        logger.info("Received update request for {}/{} in rev {}", entityName, id, rev);
        logger.debug("Payload for update request {}/{} in rev {}: {}", entityName, id, rev, payload);
        try {
            GraphIndexing.GraphIndexationSpec spec = new GraphIndexing.GraphIndexationSpec();
            spec.setJsonOrJsonLdPayload(payload).setPermissionGroup(organization).setEntityName(entityName).setId(id).setRevision(rev).setDefaultNamespace(buildDefaultNamespace(organization, domain, schema, schemaVersion));
            indexer.updateJsonOrJsonLd(spec);
            return ResponseEntity.ok(null);
        } catch (JSONException | JsonLdError e) {
            logger.warn(String.format("UPD: Was not able to process the payload %s", payload), e);
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch(Exception e){
            logger.error(String.format("UPD: Was not able to update the instance %s with the payload %s", entityName, payload), e);
            throw new RuntimeException(e);
        }
    }

    @DeleteMapping(value="/{organization}/{domain}/{schema}/{schemaversion}/{id}", produces = MediaType.APPLICATION_JSON)
    public ResponseEntity<String> deleteInstance(@PathVariable("organization") String organization, @PathVariable("domain") String domain, @PathVariable("schema") String schema, @PathVariable("schemaversion") String schemaVersion, @PathVariable("id") String id, @RequestAttribute(value="rev", required = false) Integer rev, @RequestParam(value = "authorId", required = false) String authorId, @RequestParam(value = "timestamp", required = false) String timestamp) throws IOException {
        String entityName = buildEntityName(organization, domain, schema, schemaVersion);
        logger.info("Received delete request for {}/{} in rev {}", entityName, id, rev);
        try {
            indexer.delete(entityName, id, rev);
            return ResponseEntity.ok(null);
        } catch(Exception e){
            logger.error(String.format("DEL: Was not able to delete the instance %s", entityName), e);
            throw new RuntimeException(e);
        }
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
