package org.humanbrainproject.knowledgegraph.suggestion.api;

import org.humanbrainproject.knowledgegraph.annotations.ToBeTested;
import org.humanbrainproject.knowledgegraph.commons.api.RestUtils;
import org.humanbrainproject.knowledgegraph.commons.authorization.control.AuthorizationContext;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusSchemaReference;
import org.humanbrainproject.knowledgegraph.query.entity.Pagination;
import org.humanbrainproject.knowledgegraph.query.entity.QueryResult;
import org.humanbrainproject.knowledgegraph.suggestion.boundary.Suggest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.Map;

import static org.humanbrainproject.knowledgegraph.commons.api.ParameterConstants.*;

@RestController
@RequestMapping(value = "/api/suggestion", produces = MediaType.APPLICATION_JSON)
@ToBeTested(easy = true)
public class SuggestionAPI {

    @Autowired
    AuthorizationContext authorizationContext;

    @Autowired
    Suggest suggest;


    private Logger logger = LoggerFactory.getLogger(SuggestionAPI.class);


    @PostMapping(value="/{"+ ORG+"}/{"+DOMAIN+"}/{"+SCHEMA+"}/{"+VERSION+"}/fields", consumes = {MediaType.APPLICATION_JSON, RestUtils.APPLICATION_LD_JSON}, produces = MediaType.APPLICATION_JSON)
    public ResponseEntity<QueryResult<List<Map>>> getStructureForSchemaByField(@RequestBody(required = false) String payload, @PathVariable(ORG) String org, @PathVariable(DOMAIN) String domain, @PathVariable(SCHEMA) String schema, @PathVariable(VERSION) String version, @RequestParam(value = "field") String field, @RequestParam(value = SEARCH, required = false) String search, @RequestParam(value = SIZE, required = false) Integer size, @RequestParam(value = START, required = false) Integer start, @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization){
        authorizationContext.populateAuthorizationContext(authorization);

        NexusSchemaReference schemaReference = new NexusSchemaReference(org, domain, schema, version);

        Pagination pagination = new Pagination();
        pagination.setStart(start==null ? 0 : start);
        pagination.setSize(size);
        logger.info(String.format("Loading suggestion for object %s and field %s", schemaReference, field));
        logger.info(String.format("Payload: %s", payload));
        return ResponseEntity.ok(suggest.suggestByField(schemaReference, field, search, pagination));
    }

}
