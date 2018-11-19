package org.humanbrainproject.knowledgegraph.indexing.api;

import com.github.jsonldjava.core.JsonLdError;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.humanbrainproject.knowledgegraph.indexing.boundary.GraphIndexing;
import org.humanbrainproject.knowledgegraph.indexing.entity.IndexingMessage;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusInstanceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.ws.rs.core.MediaType;

@RestController
@RequestMapping(value = "/indexing")
@Api(value="/indexing", description = "The indexing api to upload JSON-LD to the arango database")
public class IndexingAPI {

    @Autowired
    GraphIndexing indexer;

    private Logger logger = LoggerFactory.getLogger(IndexingAPI.class);

    @ApiOperation("Creates a new instance")
    @PostMapping(value="/{organization}/{domain}/{schema}/{schemaversion}/{id}", consumes = {MediaType.APPLICATION_JSON, "application/ld+json"}, produces = MediaType.APPLICATION_JSON)
    public ResponseEntity<String> addInstance(@RequestBody String payload, @PathVariable("organization") String organization, @PathVariable("domain") String domain, @PathVariable("schema") String schema, @PathVariable("schemaversion") String schemaVersion, @PathVariable("id") String id, @RequestParam(value = "authorId", required = false) String authorId, @RequestParam(value = "eventDateTime", required = false) String timestamp) {
        NexusInstanceReference path = new NexusInstanceReference(organization, domain, schema, schemaVersion, id);
        logger.info("Received insert request for {}", path.getRelativeUrl().getUrl());
        logger.debug("Payload for insert request {}: {}", path.getRelativeUrl().getUrl(), payload);
        try {
            IndexingMessage message = new IndexingMessage(path, payload, timestamp, authorId);
            indexer.insert(message);
            return ResponseEntity.ok(null);
        } catch (JsonLdError e) {
            logger.warn(String.format("INS: Was not able to process the payload %s", payload), e);
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch(Exception e){
            logger.error(String.format("INS: Was not able to insert the instance %s with the payload %s", path.getRelativeUrl().getUrl(), payload), e);
            throw new RuntimeException(e);
        }
    }

    @PutMapping(value="/{organization}/{domain}/{schema}/{schemaversion}/{id}/{rev}", consumes = {MediaType.APPLICATION_JSON, "application/ld+json"}, produces = MediaType.APPLICATION_JSON)
    public ResponseEntity<String> updateInstance(@RequestBody String payload, @PathVariable("organization") String organization, @PathVariable("domain") String domain, @PathVariable("schema") String schema, @PathVariable("schemaversion") String schemaVersion, @PathVariable("id") String id, @PathVariable("rev") Integer rev, @RequestParam(value = "authorId", required = false) String authorId, @RequestParam(value = "eventDateTime", required = false) String timestamp) {
        NexusInstanceReference path = new NexusInstanceReference(organization, domain, schema, schemaVersion, id).setRevision(rev);
        logger.info("Received update request for {} in rev {}", path.getRelativeUrl().getUrl(), rev);
        logger.debug("Payload for update request {} in rev {}: {}", path.getRelativeUrl().getUrl(), rev, payload);
        try {
            IndexingMessage message = new IndexingMessage(path, payload, timestamp, authorId);
            indexer.update(message);
            return ResponseEntity.ok(null);
        } catch (JsonLdError  e) {
            logger.warn(String.format("UPD: Was not able to process the payload %s", payload), e);
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch(Exception e){
            logger.error(String.format("UPD: Was not able to update the instance %s with the payload %s", path.getRelativeUrl().getUrl(), payload), e);
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    @DeleteMapping(value="/{organization}/{domain}/{schema}/{schemaversion}/{id}", produces = MediaType.APPLICATION_JSON)
    public ResponseEntity<String> deleteInstance(@PathVariable("organization") String organization, @PathVariable("domain") String domain, @PathVariable("schema") String schema, @PathVariable("schemaversion") String schemaVersion, @PathVariable("id") String id, @RequestAttribute(value="rev", required = false) Integer rev, @RequestParam(value = "authorId", required = false) String authorId, @RequestParam(value = "eventDateTime", required = false) String timestamp) {
        NexusInstanceReference path = new NexusInstanceReference(organization, domain, schema, schemaVersion, id).setRevision(rev);
        logger.info("Received delete request for {} in rev {}", path.getRelativeUrl(), id, rev);
        try {
            indexer.delete(path);
            return ResponseEntity.ok(String.format("Successfully deleted the instance %s", path.getRelativeUrl().getUrl()));
        }
        catch(Exception e){
            logger.error(String.format("DEL: Was not able to delete the instance %s", path.getRelativeUrl().getUrl() ), e);
            throw new RuntimeException(e);
        }
    }

    @DeleteMapping("/collections")
    public void clearGraph(){
        indexer.clearGraph();
    }

}
