package org.humanbrainproject.knowledgegraph.query.api;

import io.swagger.annotations.Api;
import org.humanbrainproject.knowledgegraph.annotations.ToBeTested;
import org.humanbrainproject.knowledgegraph.commons.InternalApi;
import org.humanbrainproject.knowledgegraph.commons.authorization.control.AuthorizationContext;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoCollectionReference;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusInstanceReference;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusSchemaReference;
import org.humanbrainproject.knowledgegraph.instances.boundary.Instances;
import org.humanbrainproject.knowledgegraph.query.boundary.ArangoGraph;
import org.humanbrainproject.knowledgegraph.query.entity.Pagination;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;

import javax.ws.rs.core.MediaType;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.humanbrainproject.knowledgegraph.commons.api.ParameterConstants.*;

@RestController
@RequestMapping(value = "/arango", produces = MediaType.APPLICATION_JSON)
@Api(value="/arango", description = "The API for reflecting the knowledge graph")
@InternalApi
@ToBeTested(easy = true)
public class GraphInternalAPI {

    @Autowired
    AuthorizationContext authorizationContext;

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
    @GetMapping(value = "/graph/{"+ORG+"}/{"+DOMAIN+"}/{"+SCHEMA+"}/{"+VERSION+"}/{"+ID+"}", consumes = { MediaType.WILDCARD})
    public ResponseEntity<Map> getGraph(@PathVariable(ORG) String org, @PathVariable(DOMAIN) String domain, @PathVariable(SCHEMA) String schema, @PathVariable(VERSION) String version, @PathVariable(ID) String id, @RequestParam(value= "step", required = false, defaultValue = "2") Integer step, @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        try{
            authorizationContext.populateAuthorizationContext(authorization);

            NexusInstanceReference instanceReference = new NexusInstanceReference(org, domain, schema, version, id);
            //TODO Validate step value
            return ResponseEntity.ok(graph.getGraph(instanceReference, step));
        } catch (HttpClientErrorException e){
            return ResponseEntity.status(e.getStatusCode()).build();
        }
    }

    @GetMapping(value = "/instances/{"+ORG+"}/{"+DOMAIN+"}/{"+SCHEMA+"}/{"+VERSION+"}", consumes = { MediaType.WILDCARD})
    public ResponseEntity<Map> getInstanceList(@PathVariable(ORG) String org, @PathVariable(DOMAIN) String domain, @PathVariable(SCHEMA) String schema, @PathVariable(VERSION) String version, @RequestParam(value = SIZE, required = false) Integer size, @RequestParam(value = "from", required = false, defaultValue = "0") Integer from, @RequestParam(value = SEARCH, required = false) String searchTerm, @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) throws Exception{
        authorizationContext.populateAuthorizationContext(authorization);
        try{
            NexusSchemaReference schemaReference = new NexusSchemaReference(org, domain, schema, version);
            return ResponseEntity.ok(graph.getInstanceList(schemaReference, searchTerm, new Pagination().setStart(from).setSize(size)));
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
    @GetMapping(value = "/instances/{"+ORG+"}/{"+DOMAIN+"}/{"+SCHEMA+"}/{"+VERSION+"}/{"+ID+"}", consumes = { MediaType.WILDCARD})
    @Deprecated
    public ResponseEntity<Map> getInstance(@PathVariable(ORG) String org, @PathVariable(DOMAIN) String domain, @PathVariable(SCHEMA) String schema, @PathVariable(VERSION) String version, @PathVariable(ID) String id, @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization ) {
        authorizationContext.populateAuthorizationContext(authorization);
        try{
            Map instance = instances.getInstance(new NexusInstanceReference(org, domain, schema, version, id));
            return instance!=null ? ResponseEntity.ok(instance) : ResponseEntity.notFound().build();
        } catch (HttpClientErrorException e){
            return ResponseEntity.status(e.getStatusCode()).build();
        }
    }

    @GetMapping(value = "/internalDocuments/{collection}", consumes = { MediaType.WILDCARD})
    public ResponseEntity<List<Map>> getInternalDocuments(@PathVariable("collection") String col) {
        authorizationContext.setMasterCredential();
        try{
            List<Map> rootList = graph.getInternalDocuments(new ArangoCollectionReference(col));
            if(rootList.isEmpty()){
                rootList = Collections.emptyList();
            }
            return ResponseEntity.ok(rootList);
        } catch (HttpClientErrorException e){
            return ResponseEntity.status(e.getStatusCode()).build();
        }
    }

    @GetMapping(value = "/bookmarks/{"+ORG+"}/{"+DOMAIN+"}/{"+SCHEMA+"}/{"+VERSION+"}/{"+ID+"}", consumes = { MediaType.WILDCARD})
    public ResponseEntity<Map> getBookmarks(@PathVariable(ORG) String org, @PathVariable(DOMAIN) String domain, @PathVariable(SCHEMA) String schema, @PathVariable(VERSION) String version, @PathVariable(ID) String id, @RequestParam(value = SIZE, required = false) Integer size, @RequestParam(value = "from", required = false, defaultValue = "0") Integer from, @RequestParam(value = SEARCH, required = false) String searchTerm, @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        authorizationContext.populateAuthorizationContext(authorization);
        try{
            NexusInstanceReference instanceReference = new NexusInstanceReference(org, domain, schema, version, id);
            Map bookmarks = graph.getBookmarks(instanceReference, searchTerm, new Pagination().setSize(size).setStart(from));
            if(bookmarks==null){
                bookmarks = Collections.emptyMap();
            }
            return ResponseEntity.ok(bookmarks);
        } catch (HttpClientErrorException e){
            return ResponseEntity.status(e.getStatusCode()).build();
        }
    }
}
