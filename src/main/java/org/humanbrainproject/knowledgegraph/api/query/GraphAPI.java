package org.humanbrainproject.knowledgegraph.api.query;

import io.swagger.annotations.Api;
import org.humanbrainproject.knowledgegraph.boundary.graph.ArangoGraph;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;

import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(value = "/arango/graph", produces = MediaType.APPLICATION_JSON)
@Api(value="/arango/graph", description = "The API for reflecting the knowledge graph")
public class GraphAPI {

    @Autowired
    ArangoGraph graph;

    @GetMapping(value = "/{org}/{domain}/{schema}/{version}/{id}", consumes = { MediaType.WILDCARD})
    public ResponseEntity<List<Map>> getGraph(@PathVariable("org") String org, @PathVariable("domain") String domain, @PathVariable("schema") String schema, @PathVariable("version") String version, @PathVariable("id") String id, @RequestParam(value= "step", required = false, defaultValue = "2") Integer step) throws Exception{
        try{
            String v = version.replaceAll("\\.", "_");
            String vert =  String.format("%s-%s-%s-%s/%s", org,domain, schema, v, id);
            //TODO Validate step value
            return ResponseEntity.ok(graph.getGraph(vert, step));
        } catch (HttpClientErrorException e){
            return ResponseEntity.status(e.getStatusCode()).build();
        }
    }

}
