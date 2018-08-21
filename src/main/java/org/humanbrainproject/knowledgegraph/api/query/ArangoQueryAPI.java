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
@RequestMapping(value = "/arango", produces = MediaType.APPLICATION_JSON)
@Api(value="/arango", description = "The API for querying the knowledge graph")
public class ArangoQueryAPI {

    @Autowired
    ArangoQuery query;

    @Autowired
    Templating templating;

    @PostMapping(value="/query", consumes = {MediaType.APPLICATION_JSON, "application/ld+json"})
    public ResponseEntity<QueryResult> queryPropertyGraphBySpecification(@RequestBody String payload, @RequestParam(value = "usecontext", required = false, defaultValue = "false") boolean useContext, @RequestParam(value = "size", required = false) Integer size, @RequestParam(value = "start", required = false) Integer start, @RequestHeader(value = "Authorization", required = false) String authorization) throws Exception {
        try {
            return ResponseEntity.ok(query.queryPropertyGraphBySpecification(payload, useContext, authorization, size, start));
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

    @PutMapping(value="/query/{id}", consumes = {MediaType.APPLICATION_JSON, "application/ld+json"})
    public ResponseEntity<Void> saveSpecificationToDB(@RequestBody String payload, @PathVariable("id") String id, @RequestHeader(value = "Authorization", required = false) String authorization) throws Exception {
        try {
            query.storeSpecificationInDb(payload, id, authorization);
            return null;
        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).build();
        }
    }

    @PostMapping(value = "/query/reflect", consumes = {MediaType.APPLICATION_JSON, "application/ld+json"})
    public ResponseEntity<QueryResult> reflectSpecification(@RequestBody String payload, @RequestParam(value = "usecontext", required = false, defaultValue = "false") boolean useContext, @RequestParam(value = "size", required = false) Integer size, @RequestParam(value = "start", required = false) Integer start, @RequestHeader(value = "Authorization", required = false) String authorization) throws Exception {
        try {
            return ResponseEntity.ok(query.queryPropertyGraphBySpecification(payload, useContext, authorization, size, start));
        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).build();
        }
    }

    @PostMapping(value="/query/meta", consumes = {MediaType.APPLICATION_JSON, "application/ld+json"})
    public ResponseEntity<QueryResult> metaSpecification(@RequestBody String payload, @RequestHeader(value = "Authorization", required = false) String authorization) throws Exception {
        try {
            return ResponseEntity.ok(query.metaQueryBySpecification(payload, authorization));
        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).build();
        }
    }

    @GetMapping("/query/{id}/meta")
    public ResponseEntity<QueryResult> executeMetaQuery(@PathVariable("id") String id, @RequestHeader(value = "Authorization", required = false) String authorization) throws Exception {
        try {
            return ResponseEntity.ok(query.metaQueryPropertyGraphByStoredSpecification(id, authorization));
        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).build();
        }
    }

    @PostMapping(value = "/query/{id}/template", consumes = {MediaType.TEXT_PLAIN})
    public ResponseEntity<String> applyFreemarkerTemplateToApi(@RequestBody String template, @PathVariable("id") String id,  @RequestParam(value = "size", required = false) Integer size, @RequestParam(value = "start", required = false) Integer start, @RequestParam(value= "lib", required = false) String library, @RequestHeader(value = "Authorization", required = false) String authorization) throws Exception {
        try {
            String body = query.queryPropertyGraphByStoredSpecificationAndFreemarkerTemplate(id, template, authorization, size, start, library);
            return ResponseEntity.ok(body);
        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).build();
        }
    }
    @PostMapping(value = "/query/{id}/template/meta", consumes = {MediaType.TEXT_PLAIN})
    public ResponseEntity<String> applyFreemarkerTemplateToMetaApi(@RequestBody String template, @PathVariable("id") String id,  @RequestHeader(value = "Authorization", required = false) String authorization) throws Exception {
        try {
            String body = query.metaQueryPropertyGraphByStoredSpecificationAndFreemarkerTemplate(id, template, authorization);
            return ResponseEntity.ok(body);
        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).build();
        }
    }

    @PutMapping(value = "/query/{queryId}/template/{templateId}", consumes = {MediaType.TEXT_PLAIN})
    public ResponseEntity<Void> saveFreemarkerTemplate(@RequestBody String template, @PathVariable("queryId") String queryId, @PathVariable("templateId") String templateId, @RequestParam(value= "lib", required=false) String library) throws Exception {
        Template t = new Template();
        t.setTemplateContent(template);
        t.setLibrary(library==null ? templateId : library);
        t.set_key(String.format("%s_%s", queryId, templateId));
        templating.saveTemplate(t);
        return null;
    }


    @GetMapping(value = "/query/{queryId}/template/{templateId}")
    public ResponseEntity<String> executeQueryBasedOnTemplate(@PathVariable("queryId") String queryId, @PathVariable("templateId") String templateId, @RequestParam(value = "size", required = false) Integer size, @RequestParam(value = "start", required = false) Integer start, @RequestHeader(value = "Authorization", required = false) String authorization) throws Exception {
        Template template = templating.getTemplateById(String.format("%s_%s", queryId, templateId));
        return applyFreemarkerTemplateToApi(template.getTemplateContent(), queryId, size, start, template.getLibrary(), authorization);
    }


    @GetMapping(value = "/query/{queryId}/template/{templateId}/meta")
    public ResponseEntity<String> executeMetaQueryBasedOnTemplate(@PathVariable("queryId") String queryId, @PathVariable("templateId") String templateId, @RequestHeader(value = "Authorization", required = false) String authorization) throws Exception {
        Template template = templating.getTemplateById(String.format("%s_%s", queryId, templateId));
        return applyFreemarkerTemplateToMetaApi(template.getTemplateContent(), queryId, authorization);
    }


    @PostMapping(value = "/query/{queryId}/template/{templateId}/meta", consumes = {MediaType.TEXT_PLAIN})
    public ResponseEntity<String> applyFreemarkerOnMetaQueryBasedOnTemplate(@RequestBody String template, @PathVariable("queryId") String queryId, @PathVariable("templateId") String templateId, @RequestHeader(value = "Authorization", required = false) String authorization) throws Exception {
        Template metaTemplate = templating.getTemplateById(String.format("%s_%s", queryId, templateId));
        String s = query.applyFreemarkerOnMetaQueryBasedOnTemplate(metaTemplate.getTemplateContent(), template, queryId, authorization);
        return ResponseEntity.ok(s);
    }


    @PutMapping(value = "/libraries/{libraryId}", consumes = {MediaType.TEXT_PLAIN})
    public ResponseEntity<Void> saveFreemarkerLibrary(@RequestBody String template, @PathVariable("libraryId") String libraryId) throws Exception {
        Template t = new Template();
        t.setTemplateContent(template);
        t.set_key(libraryId);
        templating.saveLibrary(t);
        return null;
    }


}
