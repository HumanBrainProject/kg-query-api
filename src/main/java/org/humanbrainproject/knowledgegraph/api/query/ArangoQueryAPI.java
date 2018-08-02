package org.humanbrainproject.knowledgegraph.api.query;

import org.humanbrainproject.knowledgegraph.boundary.query.ArangoQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;

import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.logging.Handler;

@RestController
@RequestMapping(value = "/arango", consumes = {MediaType.APPLICATION_JSON, "application/ld+json"}, produces = MediaType.APPLICATION_JSON)
public class ArangoQueryAPI {

    @Autowired
    ArangoQuery query;


    @PostMapping("/query")
    public ResponseEntity<List<Object>> queryPropertyGraphBySpecification(@RequestBody String payload, @RequestParam(value = "usecontext", required = false, defaultValue = "false") boolean useContext, @RequestParam(value = "size", required = false) Integer size, @RequestParam(value = "start", required = false) Integer start, @RequestHeader(value = "Authorization", required = false) String authorization) throws Exception {
        try {
            return ResponseEntity.ok(query.queryPropertyGraphBySpecification(payload, useContext, authorization, size, start));
        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).build();
        }
    }

    @PostMapping("/query/{id}")
    public ResponseEntity<Void> saveSpecificationToDB(@RequestBody String payload, @PathVariable("id") String id, @RequestHeader(value = "Authorization", required = false) String authorization) throws Exception {
        try {
            query.storeSpecificationInDb(payload, id, authorization);
            return null;
        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).build();
        }
    }

    @GetMapping("/query/{id}")
    public ResponseEntity<List<Object>> executeStoredQuery(@PathVariable("id") String id, @RequestParam(value = "usecontext", required = false, defaultValue = "false") boolean useContext,  @RequestParam(value = "size", required = false) Integer size, @RequestParam(value = "start", required = false) Integer start, @RequestHeader(value = "Authorization", required = false) String authorization) throws Exception {
        try {
            return ResponseEntity.ok(query.queryPropertyGraphByStoredSpecification(id, useContext, authorization, size, start));
        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).build();
        }
    }


    @PostMapping(value = "/query/{id}/templates", consumes = {MediaType.TEXT_PLAIN})
    public ResponseEntity<List<Object>> applyTemplateToApi(@RequestBody String template, @PathVariable("id") String id,  @RequestParam(value = "size", required = false) Integer size, @RequestParam(value = "start", required = false) Integer start, @RequestHeader(value = "Authorization", required = false) String authorization) throws Exception {
        try {
            return ResponseEntity.ok(query.queryPropertyGraphByStoredSpecificationAndTemplate(id, template, authorization, size, start));
        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).build();
        }
    }


}
