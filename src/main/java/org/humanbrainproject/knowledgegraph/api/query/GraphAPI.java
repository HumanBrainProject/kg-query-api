package org.humanbrainproject.knowledgegraph.api.query;

import com.arangodb.entity.AqlFunctionEntity;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.Api;
import org.humanbrainproject.knowledgegraph.boundary.graph.ArangoGraph;
import org.humanbrainproject.knowledgegraph.boundary.query.ArangoQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.configurationprocessor.json.JSONArray;
import org.springframework.boot.configurationprocessor.json.JSONException;
import org.springframework.boot.configurationprocessor.json.JSONObject;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;

import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.util.*;
import java.util.function.Predicate;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@RestController
@RequestMapping(value = "/arango", produces = MediaType.APPLICATION_JSON)
@Api(value="/arango/graph", description = "The API for reflecting the knowledge graph")
public class GraphAPI {

    @Autowired
    ArangoGraph graph;

    @GetMapping(value = "/graph/{org}/{domain}/{schema}/{version}/{id}", consumes = { MediaType.WILDCARD})
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

    @GetMapping(value = "/release/{org}/{domain}/{schema}/{version}/{id}", consumes = { MediaType.WILDCARD})
    public ResponseEntity<Map<String,Object>> getReleaseGraph(@PathVariable("org") String org, @PathVariable("domain") String domain, @PathVariable("schema") String schema, @PathVariable("version") String version, @PathVariable("id") String id) throws Exception{
        try{
            String v = version.replaceAll("\\.", "_");
            String vert =  String.format("%s-%s-%s-%s/%s", org,domain, schema, v, id);
            List<Map> rootList = graph.getDocument(vert);
            if(rootList.isEmpty()){
                throw new Exception("Document not found");
            }
            Map root = rootList.get(0);
            List<Map> res = graph.getReleaseGraph(vert, Optional.empty());
            root.put("children", res);
            return ResponseEntity.ok(root);

        } catch (HttpClientErrorException e){
            return ResponseEntity.status(e.getStatusCode()).build();
        }
    }

    @GetMapping(value = "/instances/{org}/{domain}/{schema}/{version}", consumes = { MediaType.WILDCARD})
    public ResponseEntity<Map> getInstanceList(@PathVariable("org") String org, @PathVariable("domain") String domain, @PathVariable("schema") String schema, @PathVariable("version") String version, @RequestParam(value = "size", required = false) Integer size, @RequestParam(value = "from", required = false) Integer from, @RequestParam(value = "search", required = false) String searchTerm) throws Exception{
        try{
            String v = version.replaceAll("\\.", "_");
            String collection =  String.format("%s-%s-%s-%s", org,domain, schema, v);
            return ResponseEntity.ok(graph.getInstanceList(collection,from, size, searchTerm));
        } catch (HttpClientErrorException e){
            return ResponseEntity.status(e.getStatusCode()).build();
        }
    }

    @GetMapping(value = "/releasestatus/{org}/{domain}/{schema}/{version}/{id}", consumes = { MediaType.WILDCARD})
    public ResponseEntity<Map<String,Object>> getReleaseStatus(@PathVariable("org") String org, @PathVariable("domain") String domain, @PathVariable("schema") String schema, @PathVariable("version") String version, @PathVariable("id") String id) throws Exception{
        try{
            String v = version.replaceAll("\\.", "_");
            String vert =  String.format("%s-%s-%s-%s/%s", org,domain, schema, v, id);
            String reconciledId =  String.format("%sreconciled-%s-%s-%s/%s", org,domain, schema, v, id);
            List<Map> rootList = graph.getReleaseStatus(vert, reconciledId);
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
            List<Map> rootList = graph.getGetEditorSpecDocument(col);
            if(rootList.isEmpty()){
                throw new Exception("Document not found");
            }
            return ResponseEntity.ok(rootList);
        } catch (HttpClientErrorException e){
            return ResponseEntity.status(e.getStatusCode()).build();
        }
    }

    @GetMapping(value = "/bookmarkList/{org}/{domain}/{schema}/{version}/{id}", consumes = { MediaType.WILDCARD})
    public ResponseEntity<List<Map>> getReleaseGraph(@PathVariable("org") String org, @PathVariable("domain") String domain, @PathVariable("schema") String schema, @PathVariable("version") String version, @PathVariable("id") String id, @RequestParam(value = "size", required = false) Integer size, @RequestParam(value = "start", required = false) Integer start, @RequestParam(value="search", required = false) String searchTerm) throws Exception{
        try{
            String bookmarkListId =  String.format("%s/%s/%s/%s/%s", org,domain, schema, version, id);
            List<Map> rootList = graph.getInstancesFromBookMarkList(bookmarkListId, start, size, searchTerm);
            return ResponseEntity.ok(rootList);
        } catch (HttpClientErrorException e){
            return ResponseEntity.status(e.getStatusCode()).build();
        }
    }

}
