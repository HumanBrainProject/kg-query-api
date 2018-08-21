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

import javax.ws.rs.core.MediaType;

@RestController
@RequestMapping(value = "/meta/query", produces = MediaType.APPLICATION_JSON)
@Api(value="/meta/query", description = "The API for querying metainformation of the knowledge graph")
public class MetaQueryAPI {

    @Autowired
    ArangoQuery query;

    @Autowired
    Templating templating;

    @PostMapping(consumes = {MediaType.APPLICATION_JSON, "application/ld+json"})
    public ResponseEntity<QueryResult> metaSpecification(@RequestBody String payload, @RequestHeader(value = "Authorization", required = false) String authorization) throws Exception {
        try {
            return ResponseEntity.ok(query.metaQueryBySpecification(payload, authorization));
        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).build();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<QueryResult> executeMetaQuery(@PathVariable("id") String id, @RequestHeader(value = "Authorization", required = false) String authorization) throws Exception {
        try {
            return ResponseEntity.ok(query.metaQueryPropertyGraphByStoredSpecification(id, authorization));
        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).build();
        }
    }

    @PostMapping(value = "/{id}/template", consumes = {MediaType.TEXT_PLAIN})
    public ResponseEntity<String> applyFreemarkerTemplateToMetaApi(@RequestBody String template, @PathVariable("id") String id,  @RequestHeader(value = "Authorization", required = false) String authorization) throws Exception {
        try {
            String body = query.metaQueryPropertyGraphByStoredSpecificationAndFreemarkerTemplate(id, template, authorization);
            return ResponseEntity.ok(body);
        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).build();
        }
    }

    @GetMapping(value = "/{queryId}/template/{templateId}")
    public ResponseEntity<String> executeMetaQueryBasedOnTemplate(@PathVariable("queryId") String queryId, @PathVariable("templateId") String templateId, @RequestHeader(value = "Authorization", required = false) String authorization) throws Exception {
        Template template = templating.getTemplateById(String.format("%s_%s", queryId, templateId));
        return applyFreemarkerTemplateToMetaApi(template.getTemplateContent(), queryId, authorization);
    }


    @PostMapping(value = "/{queryId}/template/{templateId}", consumes = {MediaType.TEXT_PLAIN})
    public ResponseEntity<String> applyFreemarkerOnMetaQueryBasedOnTemplate(@RequestBody String template, @PathVariable("queryId") String queryId, @PathVariable("templateId") String templateId, @RequestHeader(value = "Authorization", required = false) String authorization) throws Exception {
        Template metaTemplate = templating.getTemplateById(String.format("%s_%s", queryId, templateId));
        String s = query.applyFreemarkerOnMetaQueryBasedOnTemplate(metaTemplate.getTemplateContent(), template, queryId, authorization);
        return ResponseEntity.ok(s);
    }




}
