package org.humanbrainproject.knowledgegraph.api.query;

import io.swagger.annotations.Api;
import org.humanbrainproject.knowledgegraph.boundary.query.ArangoQuery;
import org.humanbrainproject.knowledgegraph.boundary.query.Templating;
import org.humanbrainproject.knowledgegraph.entity.Template;
import org.humanbrainproject.knowledgegraph.entity.query.QueryResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;

import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import java.util.List;

@RestController
@RequestMapping(value = "/query", produces = MediaType.APPLICATION_JSON)
@Api(value="/query", description = "The API for querying the knowledge graph")
public class QueryAPI {

    @Autowired
    ArangoQuery query;

    @Autowired
    Templating templating;

    @PostMapping(consumes = {MediaType.APPLICATION_JSON, "application/ld+json"})
    public ResponseEntity<QueryResult> queryPropertyGraphBySpecification(@RequestBody String payload, @RequestParam(value = "usecontext", required = false, defaultValue = "false") boolean useContext, @RequestParam(value = "size", required = false) Integer size, @RequestParam(value = "start", required = false) Integer start, @RequestHeader(value = "Authorization", required = false) String authorization) throws Exception {
        try {
            return ResponseEntity.ok(query.queryPropertyGraphBySpecification(payload, useContext, authorization, size, start));
        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).build();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<QueryResult> executeStoredQuery(@PathVariable("id") String id, @RequestParam(value = "usecontext", required = false, defaultValue = "false") boolean useContext,  @RequestParam(value = "size", required = false) Integer size, @RequestParam(value = "start", required = false) Integer start, @RequestHeader(value = "Authorization", required = false) String authorization) throws Exception {
        try {
            return ResponseEntity.ok(query.queryPropertyGraphByStoredSpecification(id, useContext, authorization, size, start));
        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).build();
        }
    }

    @PutMapping(value="/{id}", consumes = {MediaType.APPLICATION_JSON, "application/ld+json"})
    public ResponseEntity<Void> saveSpecificationToDB(@RequestBody String payload, @PathVariable("id") String id, @RequestHeader(value = "Authorization", required = false) String authorization) throws Exception {
        try {
            query.storeSpecificationInDb(payload, id, authorization);
            return null;
        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).build();
        }
    }

    @PostMapping(value = "/{id}/template", consumes = {MediaType.TEXT_PLAIN})
    public ResponseEntity<String> applyFreemarkerTemplateToApi(@RequestBody String template, @PathVariable("id") String id,  @RequestParam(value = "size", required = false) Integer size, @RequestParam(value = "start", required = false) Integer start, @RequestParam(value= "lib", required = false) String library, @RequestHeader(value = "Authorization", required = false) String authorization) throws Exception {
        try {
            String body = query.queryPropertyGraphByStoredSpecificationAndFreemarkerTemplate(id, template, authorization, size, start, library);
            return ResponseEntity.ok(body);
        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).build();
        }
    }

    @PutMapping(value = "/{queryId}/template/{templateId}", consumes = {MediaType.TEXT_PLAIN})
    public ResponseEntity<Void> saveFreemarkerTemplate(@RequestBody String template, @PathVariable("queryId") String queryId, @PathVariable("templateId") String templateId, @RequestParam(value= "lib", required=false) String library) throws Exception {
        Template t = new Template();
        t.setTemplateContent(template);
        t.setLibrary(library==null ? templateId : library);
        t.set_key(String.format("%s_%s", queryId, templateId));
        templating.saveTemplate(t);
        return null;
    }


    @GetMapping(value = "/{queryId}/template/{templateId}")
    public ResponseEntity<String> executeQueryBasedOnTemplate(@PathVariable("queryId") String queryId, @PathVariable("templateId") String templateId, @RequestParam(value = "size", required = false) Integer size, @RequestParam(value = "start", required = false) Integer start, @RequestHeader(value = "Authorization", required = false) String authorization) throws Exception {
        Template template = templating.getTemplateById(String.format("%s_%s", queryId, templateId));
        return applyFreemarkerTemplateToApi(template.getTemplateContent(), queryId, size, start, template.getLibrary(), authorization);
    }


}
