package org.humanbrainproject.knowledgegraph.query.api;

import io.swagger.annotations.Api;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoCollectionReference;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoDocumentReference;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusInstanceReference;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusSchemaReference;
import org.humanbrainproject.knowledgegraph.query.boundary.ArangoGraph;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;

import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(value = "/arango", produces = MediaType.APPLICATION_JSON)
@Api(value="/arango/graph", description = "The API for reflecting the knowledge graph")
public class GraphAPI {

    @Autowired
    ArangoGraph graph;

    @GetMapping(value = "/graph/{org}/{domain}/{schema}/{version}/{id}", consumes = { MediaType.WILDCARD})
    public ResponseEntity<List<Map>> getGraph(@PathVariable("org") String org, @PathVariable("domain") String domain, @PathVariable("schema") String schema, @PathVariable("version") String version, @PathVariable("id") String id, @RequestParam(value= "step", required = false, defaultValue = "2") Integer step) throws Exception{
        try{
            NexusInstanceReference instanceReference = new NexusInstanceReference(org, domain, schema, version, id);
            //TODO Validate step value
            return ResponseEntity.ok(graph.getGraph(instanceReference, step));
        } catch (HttpClientErrorException e){
            return ResponseEntity.status(e.getStatusCode()).build();
        }
    }

    @GetMapping(value = "/instances/{org}/{domain}/{schema}/{version}", consumes = { MediaType.WILDCARD})
    public ResponseEntity<Map> getInstanceList(@PathVariable("org") String org, @PathVariable("domain") String domain, @PathVariable("schema") String schema, @PathVariable("version") String version, @RequestParam(value = "size", required = false) Integer size, @RequestParam(value = "from", required = false, defaultValue = "0") Integer from, @RequestParam(value = "search", required = false) String searchTerm) throws Exception{
        try{
            NexusSchemaReference schemaReference = new NexusSchemaReference(org, domain, schema, version);
            return ResponseEntity.ok(graph.getInstanceList(schemaReference, from, size, searchTerm));
        } catch (HttpClientErrorException e){
            return ResponseEntity.status(e.getStatusCode()).build();
        }
    }

    @GetMapping(value = "/instance/{org}/{domain}/{schema}/{version}/{id}", consumes = { MediaType.WILDCARD})
    public ResponseEntity<Map> getInstance(@PathVariable("org") String org, @PathVariable("domain") String domain, @PathVariable("schema") String schema, @PathVariable("version") String version,@PathVariable("id") String id ) throws Exception{
        try{
            ArangoDocumentReference instanceRef = ArangoDocumentReference.fromNexusInstance(new NexusInstanceReference(org, domain, schema, version, id));
            List<Map> rootList = graph.getInstance(instanceRef);
            if(rootList.isEmpty()){
                throw new Exception("Document not found");
            }
            Map root = rootList.get(0);
            return ResponseEntity.ok(root);
        } catch (HttpClientErrorException e){
            return ResponseEntity.status(e.getStatusCode()).build();
        }
    }

    @GetMapping(value = "/document/{collection}", consumes = { MediaType.WILDCARD})
    public ResponseEntity<List<Map>> getGetEditorSpecDocument(@PathVariable("collection") String col) throws Exception{
        try{
            List<Map> rootList = graph.getGetEditorSpecDocument(new ArangoCollectionReference(col));
            if(rootList.isEmpty()){
                throw new Exception("Document not found");
            }
            return ResponseEntity.ok(rootList);
        } catch (HttpClientErrorException e){
            return ResponseEntity.status(e.getStatusCode()).build();
        }
    }

    @GetMapping(value = "/bookmarks/{org}/{domain}/{schema}/{version}/{id}", consumes = { MediaType.WILDCARD})
    public ResponseEntity<Map> getBookmarks(@PathVariable("org") String org, @PathVariable("domain") String domain, @PathVariable("schema") String schema, @PathVariable("version") String version, @PathVariable("id") String id, @RequestParam(value = "size", required = false) Integer size, @RequestParam(value = "from", required = false, defaultValue = "0") Integer from, @RequestParam(value = "search", required = false) String searchTerm) throws Exception{
        try{
            NexusInstanceReference instanceReference = new NexusInstanceReference(org, domain, schema, version, id);
            return ResponseEntity.ok(graph.getBookmarks(instanceReference, from, size, searchTerm));
        } catch (HttpClientErrorException e){
            return ResponseEntity.status(e.getStatusCode()).build();
        }
    }
}
