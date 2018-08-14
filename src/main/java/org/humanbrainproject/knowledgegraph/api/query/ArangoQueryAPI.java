package org.humanbrainproject.knowledgegraph.api.query;

import org.humanbrainproject.knowledgegraph.boundary.query.ArangoQuery;
import org.humanbrainproject.knowledgegraph.boundary.query.Templating;
import org.humanbrainproject.knowledgegraph.entity.Template;
import org.humanbrainproject.knowledgegraph.entity.query.QueryResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;

import javax.ws.rs.core.MediaType;
import java.util.List;

@RestController
@RequestMapping(value = "/arango", consumes = {MediaType.APPLICATION_JSON, "application/ld+json"}, produces = MediaType.APPLICATION_JSON)
public class ArangoQueryAPI {

    @Autowired
    ArangoQuery query;

    @Autowired
    Templating templating;


    @PostMapping("/query")
    public ResponseEntity<QueryResult> queryPropertyGraphBySpecification(@RequestBody String payload, @RequestParam(value = "usecontext", required = false, defaultValue = "false") boolean useContext, @RequestParam(value = "size", required = false) Integer size, @RequestParam(value = "start", required = false) Integer start, @RequestHeader(value = "Authorization", required = false) String authorization) throws Exception {
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
    public ResponseEntity<QueryResult> executeStoredQuery(@PathVariable("id") String id, @RequestParam(value = "usecontext", required = false, defaultValue = "false") boolean useContext,  @RequestParam(value = "size", required = false) Integer size, @RequestParam(value = "start", required = false) Integer start, @RequestHeader(value = "Authorization", required = false) String authorization) throws Exception {
        try {
            return ResponseEntity.ok(query.queryPropertyGraphByStoredSpecification(id, useContext, authorization, size, start));
        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).build();
        }
    }

    @PostMapping(value = "/query/{id}/templates/mustache", consumes = {MediaType.TEXT_PLAIN})
    public ResponseEntity<QueryResult> applyMustacheTemplateToApi(@RequestBody String template, @PathVariable("id") String id,  @RequestParam(value = "size", required = false) Integer size, @RequestParam(value = "start", required = false) Integer start, @RequestHeader(value = "Authorization", required = false) String authorization) throws Exception {
        try {
            return ResponseEntity.ok(query.queryPropertyGraphByStoredSpecificationAndMustacheTemplate(id, template, authorization, size, start));
        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).build();
        }
    }

    @PostMapping(value = "/query/{id}/templates/freemarker", consumes = {MediaType.TEXT_PLAIN})
    public ResponseEntity<String> applyFreemarkerTemplateToApi(@RequestBody String template, @PathVariable("id") String id,  @RequestParam(value = "size", required = false) Integer size, @RequestParam(value = "start", required = false) Integer start, @RequestHeader(value = "Authorization", required = false) String authorization) throws Exception {
        try {
            String body = query.queryPropertyGraphByStoredSpecificationAndFreemarkerTemplate(id, template, authorization, size, start);
            return ResponseEntity.ok(body);
        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).build();
        }
    }

    @PostMapping(value = "/query/{queryId}/freemarker/{templateId}", consumes = {MediaType.TEXT_PLAIN})
    public ResponseEntity<Void> saveFreemarkerTemplate(@RequestBody String template, @PathVariable("queryId") String queryId, @PathVariable("templateId") String templateId) throws Exception {
        Template t = new Template();
        t.setTemplateContent(template);
        t.setQueryId(queryId);
        t.set_key(templateId);
        templating.saveTemplate(t);
        return null;
    }

    @PostMapping(value = "/freemarker/libraries/{libraryId}", consumes = {MediaType.TEXT_PLAIN})
    public ResponseEntity<Void> saveFreemarkerTemplate(@RequestBody String template, @PathVariable("libraryId") String libraryId) throws Exception {
        Template t = new Template();
        t.setTemplateContent(template);
        t.set_key(libraryId);
        templating.saveLibrary(t);
        return null;
    }

    @GetMapping(value = "/query/{queryId}/freemarker/{templateId}")
    public ResponseEntity<String> executeQueryBasedOnTemplate(@PathVariable("queryId") String queryId, @PathVariable("templateId") String templateId, @RequestParam(value = "size", required = false) Integer size, @RequestParam(value = "start", required = false) Integer start, @RequestHeader(value = "Authorization", required = false) String authorization) throws Exception {
        Template template = templating.getTemplateById(templateId);
        return applyFreemarkerTemplateToApi(template.getTemplateContent(), queryId, size, start, authorization);
    }

}
