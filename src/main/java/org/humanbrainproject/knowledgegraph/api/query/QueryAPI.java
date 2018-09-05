package org.humanbrainproject.knowledgegraph.api.query;

import io.swagger.annotations.Api;
import org.humanbrainproject.knowledgegraph.boundary.query.ArangoQuery;
import org.humanbrainproject.knowledgegraph.boundary.query.Templating;
import org.humanbrainproject.knowledgegraph.entity.Template;
import org.humanbrainproject.knowledgegraph.entity.query.QueryParameters;
import org.humanbrainproject.knowledgegraph.entity.query.QueryResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import springfox.documentation.annotations.ApiIgnore;

import javax.ws.rs.core.MediaType;
import java.util.Map;

@RestController
@RequestMapping(value = "/query", produces = MediaType.APPLICATION_JSON)
@Api(value="/query", description = "The API for querying the knowledge graph")
public class QueryAPI {

    @Autowired
    ArangoQuery query;

    @Autowired
    Templating templating;

    @PostMapping(consumes = {MediaType.APPLICATION_JSON, "application/ld+json"})
    public ResponseEntity<QueryResult> queryPropertyGraphBySpecification(@RequestBody String payload, @RequestParam(value = "@vocab", required = false) String vocab, @RequestParam(value = "size", required = false) Integer size, @RequestParam(value = "start", required = false) Integer start, @RequestParam(value="orgs", required = false) String organizations, @RequestParam(value="released", required = false) boolean released, @RequestParam(value="search", required = false) String searchTerm, @RequestHeader(value = "Authorization", required = false) String authorization, @ApiIgnore @RequestParam Map<String,String> allRequestParams) throws Exception {
        try {
            QueryParameters parameters = new QueryParameters().setAllParameters(allRequestParams).setReleased(released).setStart(start).setSize(size).setVocab(vocab).setOrganizations(organizations).setAuthorizationToken(authorization).setSearchTerm(searchTerm);
            return ResponseEntity.ok(query.queryPropertyGraphBySpecification(payload, parameters));
        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).build();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<QueryResult> executeStoredQuery(@PathVariable("id") String id, @RequestParam(value = "@vocab", required = false) String vocab,  @RequestParam(value = "size", required = false) Integer size, @RequestParam(value = "start", required = false) Integer start, @RequestParam(value="orgs", required = false) String organizations, @RequestParam(value="released", required = false) boolean released, @RequestParam(value="search", required = false) String searchTerm, @RequestHeader(value = "Authorization", required = false) String authorization,  @ApiIgnore @RequestParam Map<String,String> allRequestParams) throws Exception {
        try {
            QueryParameters parameters = new QueryParameters().setAllParameters(allRequestParams).setReleased(released).setStart(start).setSize(size).setVocab(vocab).setOrganizations(organizations).setAuthorizationToken(authorization).setSearchTerm(searchTerm);
            return ResponseEntity.ok(query.queryPropertyGraphByStoredSpecification(id, parameters));
        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).build();
        }
    }

    @PutMapping(value="/{id}", consumes = {MediaType.APPLICATION_JSON, "application/ld+json"})
    public ResponseEntity<Void> saveSpecificationToDB(@RequestBody String payload, @PathVariable("id") String id, @RequestHeader(value = "Authorization", required = false) String authorization) throws Exception {
        try {
            query.storeSpecificationInDb(payload, id);
            return null;
        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).build();
        }
    }

    @PostMapping(value = "/{id}/template", consumes = {MediaType.TEXT_PLAIN})
    public ResponseEntity<QueryResult> applyFreemarkerTemplateToApi(@RequestBody String template, @PathVariable("id") String id,  @RequestParam(value = "size", required = false) Integer size, @RequestParam(value = "start", required = false) Integer start, @RequestParam(value= "lib", required = false) String library, @RequestParam(value="orgs", required = false) String organizations, @RequestParam(value="released", required = false) boolean released, @RequestHeader(value = "Authorization", required = false) String authorization,  @ApiIgnore @RequestParam Map<String,String> allRequestParams) throws Exception {
        try {
            QueryParameters parameters = new QueryParameters().setAllParameters(allRequestParams).setReleased(released).setStart(start).setSize(size).setLibrary(library).setOrganizations(organizations).setAuthorizationToken(authorization);
            QueryResult<String> result = query.queryPropertyGraphByStoredSpecificationAndFreemarkerTemplate(id, template, parameters);
            return ResponseEntity.ok(RestUtils.toJsonResultIfPossible(result));
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
    public ResponseEntity<QueryResult> executeQueryBasedOnTemplate(@PathVariable("queryId") String queryId, @PathVariable("templateId") String templateId, @RequestParam(value = "size", required = false) Integer size, @RequestParam(value = "start", required = false) Integer start, @RequestParam(value="orgs", required = false) String organizations, @RequestParam(value="released", required = false) boolean released, @RequestHeader(value = "Authorization", required = false) String authorization,  @ApiIgnore @RequestParam Map<String,String> allRequestParams) throws Exception {
        Template template = templating.getTemplateById(String.format("%s_%s", queryId, templateId));
        return applyFreemarkerTemplateToApi(template.getTemplateContent(), queryId, size, start, template.getLibrary(), organizations, released, authorization, allRequestParams);
    }


}
