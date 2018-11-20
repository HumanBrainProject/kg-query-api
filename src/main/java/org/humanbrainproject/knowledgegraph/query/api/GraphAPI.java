package org.humanbrainproject.knowledgegraph.query.api;

import io.swagger.annotations.Api;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoCollectionReference;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusInstanceReference;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusSchemaReference;
import org.humanbrainproject.knowledgegraph.instances.boundary.Instances;
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

    @Autowired
    Instances instances;

    /**
     * @deprecated Use /instances api instead
     * @param org
     * @param domain
     * @param schema
     * @param version
     * @param id
     * @param step
     * @return
     * @throws Exception
     */
    @Deprecated
    @GetMapping(value = "/graph/{org}/{domain}/{schema}/{version}/{id}", consumes = { MediaType.WILDCARD})
    public ResponseEntity<Map> getGraph(@PathVariable("org") String org, @PathVariable("domain") String domain, @PathVariable("schema") String schema, @PathVariable("version") String version, @PathVariable("id") String id, @RequestParam(value= "step", required = false, defaultValue = "2") Integer step) throws Exception{
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

    /**
     * @deprecated  Use /instances api instead
     * @param org
     * @param domain
     * @param schema
     * @param version
     * @param id
     * @return
     * @throws Exception
     */
    @GetMapping(value = "/instance/{org}/{domain}/{schema}/{version}/{id}", consumes = { MediaType.WILDCARD})
    @Deprecated
    public ResponseEntity<Map> getInstance(@PathVariable("org") String org, @PathVariable("domain") String domain, @PathVariable("schema") String schema, @PathVariable("version") String version,@PathVariable("id") String id ) throws Exception{
        try{
            Map instance = instances.getInstance(new NexusInstanceReference(org, domain, schema, version, id));
            return instance!=null ? ResponseEntity.ok(instance) : ResponseEntity.notFound().build();
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
