package org.humanbrainproject.knowledgegraph.query.api;

import io.swagger.annotations.Api;
import org.humanbrainproject.knowledgegraph.commons.InternalApi;
import org.humanbrainproject.knowledgegraph.commons.authorization.entity.OidcAccessToken;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusSchemaReference;
import org.humanbrainproject.knowledgegraph.query.boundary.ArangoQuery;
import org.humanbrainproject.knowledgegraph.query.boundary.Templating;
import org.humanbrainproject.knowledgegraph.query.entity.StoredQueryReference;
import org.humanbrainproject.knowledgegraph.query.entity.Template;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;

import javax.ws.rs.core.MediaType;

import static org.humanbrainproject.knowledgegraph.query.api.QueryAPI.*;


@RestController
@RequestMapping(value = "/internal/api/query", produces = MediaType.APPLICATION_JSON)
@Api(value = "/internal/api/query", description = "The API for querying the knowledge graph")
@InternalApi
public class QueryInternalAPI {


    @Autowired
    ArangoQuery query;

    @Autowired
    Templating templating;


    @PutMapping(value = "/{org}/{domain}/{schema}/{version}/{queryId}", consumes = {MediaType.APPLICATION_JSON, RestAPIConstants.APPLICATION_LD_JSON})
    public ResponseEntity<Void> saveSpecificationToDB(@RequestBody String payload, @PathVariable(ORG) String org, @PathVariable(DOMAIN) String domain, @PathVariable(SCHEMA) String schema, @PathVariable(VERSION) String version, @PathVariable(QUERY_ID) String id, @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) throws Exception {
        try {
            NexusSchemaReference nexusSchemaReference = new NexusSchemaReference(org, domain, schema, version);
            //TODO ensure authorization
            query.storeSpecificationInDb(payload, nexusSchemaReference, id, new OidcAccessToken().setToken(authorization));
            return null;
        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).build();
        }
    }

    @PutMapping(value = "/{org}/{domain}/{schema}/{version}/{queryId}/templates/{templateId}")
    public ResponseEntity<Void> saveFreemarkerTemplate(@RequestBody String template, @PathVariable(ORG) String org, @PathVariable(DOMAIN) String domain, @PathVariable(SCHEMA) String schema, @PathVariable(VERSION) String version, @PathVariable(QUERY_ID) String queryId, @PathVariable(TEMPLATE_ID) String templateId, @RequestParam(value = "lib", required = false) String library) {
        NexusSchemaReference schemaReference = new NexusSchemaReference(org, domain, schema, version);
        Template t = new Template(new StoredQueryReference(schemaReference, queryId), templateId, template, library == null ? templateId : library);
        templating.saveTemplate(t);
        return null;
    }

    @PutMapping(value = "/templates/{templateId}/libraries/{library}")
    public ResponseEntity<Void> saveFreemarkerLibrary(@RequestBody String library, @PathVariable(TEMPLATE_ID) String templateId, @PathVariable(LIBRARY) String libraryId) throws Exception {
        templating.saveLibrary(library, libraryId, templateId);
        return null;
    }

}
