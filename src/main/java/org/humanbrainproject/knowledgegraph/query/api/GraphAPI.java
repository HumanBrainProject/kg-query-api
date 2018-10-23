package org.humanbrainproject.knowledgegraph.query.api;

import io.swagger.annotations.Api;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusInstanceReference;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusSchemaReference;
import org.humanbrainproject.knowledgegraph.propertyGraph.arango.entity.ArangoCollectionReference;
import org.humanbrainproject.knowledgegraph.query.boundary.ArangoGraph;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;

import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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

    @GetMapping(value = "/release/{org}/{domain}/{schema}/{version}/{id}", consumes = { MediaType.WILDCARD})
    public ResponseEntity<Map<String,Object>> getReleaseGraph(@PathVariable("org") String org, @PathVariable("domain") String domain, @PathVariable("schema") String schema, @PathVariable("version") String version, @PathVariable("id") String id) throws Exception{
        try{
            NexusInstanceReference instanceReference = new NexusInstanceReference(org, domain, schema, version, id);
            List<Map> rootList = graph.getDocument(instanceReference);
            if(rootList.isEmpty()){
                throw new Exception("Document not found");
            }
            Map root = rootList.get(0);
            List<Map> res = graph.getReleaseGraph(instanceReference, Optional.empty());
            root.put("children", res);
            return ResponseEntity.ok(root);

        } catch (HttpClientErrorException e){
            return ResponseEntity.status(e.getStatusCode()).build();
        }
    }

    @GetMapping(value = "/instances/{org}/{domain}/{schema}/{version}", consumes = { MediaType.WILDCARD})
    public ResponseEntity<Map> getInstanceList(@PathVariable("org") String org, @PathVariable("domain") String domain, @PathVariable("schema") String schema, @PathVariable("version") String version, @RequestParam(value = "size", required = false) Integer size, @RequestParam(value = "from", required = false) Integer from, @RequestParam(value = "search", required = false) String searchTerm) throws Exception{
        try{
            NexusSchemaReference schemaReference = new NexusSchemaReference(org, domain, schema, version);
            return ResponseEntity.ok(graph.getInstanceList(schemaReference, from, size, searchTerm));
        } catch (HttpClientErrorException e){
            return ResponseEntity.status(e.getStatusCode()).build();
        }
    }

    @GetMapping(value = "/releasestatus/{org}/{domain}/{schema}/{version}/{id}", consumes = { MediaType.WILDCARD})
    public ResponseEntity<Map<String,Object>> getReleaseStatus(@PathVariable("org") String org, @PathVariable("domain") String domain, @PathVariable("schema") String schema, @PathVariable("version") String version, @PathVariable("id") String id) throws Exception{
        try{
            NexusInstanceReference instanceReference = new NexusInstanceReference(org, domain, schema, version, id);
            List<Map> rootList = graph.getReleaseStatus(instanceReference);
            if(rootList.isEmpty()){
                throw new Exception("Document not found");
            }
            Map root = rootList.get(0);
            return ResponseEntity.ok(root);

        } catch (HttpClientErrorException e){
            return ResponseEntity.status(e.getStatusCode()).build();
        }
    }

    //TODO rewrite
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
}
