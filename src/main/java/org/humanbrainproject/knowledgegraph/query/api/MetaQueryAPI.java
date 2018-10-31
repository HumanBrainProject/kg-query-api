package org.humanbrainproject.knowledgegraph.query.api;

import io.swagger.annotations.Api;
import org.humanbrainproject.knowledgegraph.query.boundary.ArangoQuery;
import org.humanbrainproject.knowledgegraph.query.boundary.Templating;
import org.humanbrainproject.knowledgegraph.query.entity.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
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

    @PostMapping(consumes = {MediaType.APPLICATION_JSON, RestAPIConstants.APPLICATION_LD_JSON})
    public ResponseEntity<QueryResult> metaSpecification(@RequestBody String payload, @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationToken) throws Exception {
        try {
            QueryParameters parameters = new QueryParameters(null, null);
            parameters.authorization().setToken(authorizationToken);
            return ResponseEntity.ok(query.metaQueryBySpecification(payload, parameters));
        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).build();
        }
    }

    @GetMapping("/{queryId}")
    public ResponseEntity<QueryResult> executeMetaQuery(@PathVariable("queryId") String queryId, @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationToken) throws Exception {
        try {
            StoredQueryReference storedQueryReference = new StoredQueryReference(queryId);
            QueryParameters parameters = new QueryParameters(null, null);
            parameters.authorization().setToken(authorizationToken);
            return ResponseEntity.ok(query.metaQueryPropertyGraphByStoredSpecification(storedQueryReference, parameters));
        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).build();
        }
    }

    @PostMapping(value = "/{queryId}/template", consumes = {MediaType.TEXT_PLAIN})
    public ResponseEntity<QueryResult> applyFreemarkerTemplateToMetaApi(@RequestBody String templatePayload, @PathVariable("queryId") String queryId, @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationToken) throws Exception {
        try {
            StoredQueryReference storedQueryReference = new StoredQueryReference(queryId);
            QueryParameters parameters = new QueryParameters(null, null);
            parameters.authorization().setToken(authorizationToken);
            QueryResult result = query.metaQueryPropertyGraphByStoredSpecificationAndFreemarkerTemplate(storedQueryReference, templatePayload, parameters);
            return ResponseEntity.ok(RestUtils.toJsonResultIfPossible(result));
        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).build();
        }
    }

    @GetMapping(value = "/{queryId}/template/{templateId}")
    public ResponseEntity<QueryResult> executeMetaQueryBasedOnTemplate(@PathVariable("queryId") String queryId, @PathVariable("templateId") String templateId, @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) throws Exception {
        Template template = templating.getTemplateById(new StoredQueryReference(queryId), new StoredTemplateReference(templateId));
        return applyFreemarkerTemplateToMetaApi(template.getTemplateContent(), queryId, authorization);
    }


    @PostMapping(value = "/{queryId}/template/{templateId}", consumes = {MediaType.TEXT_PLAIN})
    public ResponseEntity<QueryResult> applyFreemarkerOnMetaQueryBasedOnTemplate(@RequestBody String freemarkerTemplate, @PathVariable("queryId") String queryId, @PathVariable("templateId") String templateId, @RequestParam(value = "originalData", required = false) boolean originalData, @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationToken) throws Exception {
        Template metaTemplate = templating.getTemplateById(new StoredQueryReference(queryId), new StoredTemplateReference(templateId));
        QueryParameters parameters = new QueryParameters(null, null);
        parameters.authorization().setToken(authorizationToken);
        QueryResult result = query.applyFreemarkerOnMetaQueryBasedOnTemplate(metaTemplate.getTemplateContent(), freemarkerTemplate, new StoredQueryReference(queryId), parameters);
        return ResponseEntity.ok(RestUtils.toJsonResultIfPossible(result));
    }


}