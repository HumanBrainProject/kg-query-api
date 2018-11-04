package org.humanbrainproject.knowledgegraph.query.api;

import io.swagger.annotations.Api;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.exceptions.RootCollectionNotFoundException;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusInstanceReference;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoDocumentReference;
import org.humanbrainproject.knowledgegraph.query.boundary.ArangoQuery;
import org.humanbrainproject.knowledgegraph.query.boundary.Templating;
import org.humanbrainproject.knowledgegraph.query.entity.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import springfox.documentation.annotations.ApiIgnore;

import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(value = "/query", produces = MediaType.APPLICATION_JSON)
@Api(value="/query", description = "The API for querying the knowledge graph")
public class QueryAPI {

    private static final String TEMPLATE_ID = "templateId";
    private static final String ORG = "org";
    private static final String QUERY_ID = "queryId";
    private static final String DOMAIN = "domain";
    private static final String VOCAB = "vocab";
    private static final String SIZE = "size";
    private static final String START = "start";
    private static final String ORGS = "orgs";
    private static final String DATABASE_SCOPE = "databaseScope";
    private static final String SEARCH = "search";
    private static final String SCHEMA = "schema";
    private static final String VERSION = "version";
    private static final String INSTANCE_ID = "instanceId";
    private static final String RESTRICT_TO_ORGANIZATIONS = "restrictToOrganizations";


    @Autowired
    ArangoQuery query;

    @Autowired
    Templating templating;

    @PostMapping(consumes = {MediaType.APPLICATION_JSON, RestAPIConstants.APPLICATION_LD_JSON})
    public ResponseEntity<QueryResult> queryPropertyGraphBySpecification(@RequestBody String payload, @RequestParam(value = VOCAB, required = false) String vocab, @RequestParam(value = SIZE, required = false) Integer size, @RequestParam(value = START, required = false) Integer start, @RequestParam(value= ORGS, required = false) String organizations, @RequestParam(value= DATABASE_SCOPE, required = false) DatabaseScope databaseScope, @RequestParam(value=SEARCH, required = false) String searchTerm, @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationToken, @ApiIgnore @RequestParam Map<String,String> allRequestParams) throws Exception {
        try {
            QueryParameters parameters = new QueryParameters(databaseScope, allRequestParams);
            parameters.pagination().setStart(start).setSize(size);
            if(organizations!=null) {
                parameters.filter().restrictToOrganizations(organizations.split(","));
            }
            parameters.filter().setQueryString(searchTerm);
            parameters.resultTransformation().setVocab(vocab);
            parameters.authorization().setToken(authorizationToken);
            return ResponseEntity.ok(query.queryPropertyGraphBySpecification(payload, parameters, null));
        } catch (RootCollectionNotFoundException e){
            return ResponseEntity.notFound().build();
        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).build();
        }
    }

    @PostMapping(value="/{org}/{domain}/{schema}/{version}/{instanceId}", consumes = {MediaType.APPLICATION_JSON, RestAPIConstants.APPLICATION_LD_JSON})
    public ResponseEntity<Map> queryPropertyGraphBySpecificationWithId(@PathVariable(ORG) String org, @PathVariable(DOMAIN) String domain, @PathVariable(SCHEMA) String schema, @PathVariable(VERSION) String version, @PathVariable(INSTANCE_ID) String instanceId , @RequestBody String payload, @RequestParam(value = SIZE, required = false) Integer size, @RequestParam(value = START, required = false) Integer start, @RequestParam(value= RESTRICT_TO_ORGANIZATIONS, required = false) String restrictToOrganizations, @RequestParam(value= SEARCH, required = false) String searchTerm, @RequestParam(value= DATABASE_SCOPE, required = false) DatabaseScope databaseScope, @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) throws Exception {
        try {
            NexusInstanceReference instanceReference = new NexusInstanceReference(org, domain, schema, version, instanceId);
            QueryParameters parameters = new QueryParameters(databaseScope, null);
            parameters.pagination().setSize(size).setStart(start);
            if(restrictToOrganizations!=null) {
                parameters.filter().restrictToOrganizations(restrictToOrganizations.split(","));
            }
            parameters.filter().setQueryString(searchTerm);
            parameters.authorization().setToken(authorization);
            QueryResult<List<Map>> result = query.queryPropertyGraphBySpecification(payload, parameters, ArangoDocumentReference.fromNexusInstance(instanceReference));
            if(result.getResults().size() >= 1){
                return ResponseEntity.ok(result.getResults().get(0));
            }else{
                return ResponseEntity.noContent().build();
            }
        } catch (RootCollectionNotFoundException e){
            return ResponseEntity.notFound().build();
        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).build();
        }
    }

    @GetMapping("/{queryId}")
    public ResponseEntity<QueryResult> executeStoredQuery(@PathVariable(QUERY_ID) String queryId, @RequestParam(value = SIZE, required = false) Integer size, @RequestParam(value = START, required = false) Integer start, @RequestParam(value=RESTRICT_TO_ORGANIZATIONS, required = false) String restrictToOrganizations, @RequestParam(value=DATABASE_SCOPE, required = false) DatabaseScope databaseScope, @RequestParam(value=SEARCH, required = false) String searchTerm, @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) throws Exception {
        try {
            StoredQueryReference storedQueryReference = new StoredQueryReference(queryId);
            QueryParameters parameters = new QueryParameters(databaseScope, null);
            parameters.pagination().setSize(size).setStart(start);
            if(restrictToOrganizations!=null) {
                parameters.filter().restrictToOrganizations(restrictToOrganizations.split(","));
            }
            parameters.filter().setQueryString(searchTerm);
            parameters.authorization().setToken(authorization);
            return ResponseEntity.ok(query.queryPropertyGraphByStoredSpecification(storedQueryReference, parameters, null));
        } catch (RootCollectionNotFoundException e){
            return ResponseEntity.notFound().build();
        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).build();
        }
    }

    @GetMapping("/{queryId}/{org}/{domain}/{schema}/{version}/{instanceId}")
    public ResponseEntity<Map> executeStoredQuery(@PathVariable(QUERY_ID) String queryId, @PathVariable(ORG) String org, @PathVariable(DOMAIN) String domain, @PathVariable(SCHEMA) String schema, @PathVariable(VERSION) String version, @PathVariable(INSTANCE_ID) String instanceId, @RequestParam(value = SIZE, required = false) Integer size, @RequestParam(value = START, required = false) Integer start, @RequestParam(value=SEARCH, required = false) String searchTerm, @RequestParam(value=RESTRICT_TO_ORGANIZATIONS, required = false) String restrictToOrganizations, @RequestParam(value=DATABASE_SCOPE, required = false) DatabaseScope databaseScope, @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) throws Exception {
        try {

            NexusInstanceReference nexusInstanceReference = new NexusInstanceReference(org, domain, schema, version, instanceId);
            StoredQueryReference storedQueryReference = new StoredQueryReference(queryId);
            QueryParameters parameters = new QueryParameters(databaseScope, null);
            parameters.pagination().setSize(size).setStart(start);
            if(restrictToOrganizations!=null) {
                parameters.filter().restrictToOrganizations(restrictToOrganizations.split(","));
            }
            parameters.filter().setQueryString(searchTerm);
            parameters.authorization().setToken(authorization);
            QueryResult<List<Map>> result = query.queryPropertyGraphByStoredSpecification(storedQueryReference, parameters, ArangoDocumentReference.fromNexusInstance(nexusInstanceReference));
            if(result.getResults().size() >= 1){
                return ResponseEntity.ok(result.getResults().get(0));
            }else{
                return ResponseEntity.noContent().build();
            }

        } catch (RootCollectionNotFoundException e){
            return ResponseEntity.notFound().build();
        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).build();
        }
    }

    @PutMapping(value="/{queryId}", consumes = {MediaType.APPLICATION_JSON, RestAPIConstants.APPLICATION_LD_JSON})
    public ResponseEntity<Void> saveSpecificationToDB(@RequestBody String payload, @PathVariable(QUERY_ID) String id, @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) throws Exception {
        try {
            //TODO ensure authorization
            query.storeSpecificationInDb(payload, id);
            return null;
        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).build();
        }
    }

    @PostMapping(value = "/{queryId}/template", consumes = {MediaType.TEXT_PLAIN})
    public ResponseEntity<QueryResult> applyFreemarkerTemplateToApi(@RequestBody String templatePayload, @PathVariable(QUERY_ID) String queryId,  @RequestParam(value = SIZE, required = false) Integer size, @RequestParam(value = START, required = false) Integer start, @RequestParam(value= "lib", required = false) String library, @RequestParam(value=SEARCH, required = false) String searchTerm,  @RequestParam(value=RESTRICT_TO_ORGANIZATIONS, required = false) String restrictToOrganizations, @RequestParam(value=DATABASE_SCOPE, required = false) DatabaseScope databaseScope, @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization, @ApiIgnore @RequestParam Map<String,String> allRequestParams) throws Exception {
        try {
            StoredQueryReference storedQueryReference = new StoredQueryReference(queryId);
            QueryParameters parameters = new QueryParameters(databaseScope, null);
            parameters.pagination().setSize(size).setStart(start);
            if(restrictToOrganizations!=null) {
                parameters.filter().restrictToOrganizations(restrictToOrganizations.split(","));
            }
            parameters.context().setLibrary(new StoredLibraryReference(library));
            parameters.filter().setQueryString(searchTerm);
            parameters.authorization().setToken(authorization);
            QueryResult<String> result = query.queryPropertyGraphByStoredSpecificationAndFreemarkerTemplate(storedQueryReference, templatePayload, parameters);
            return ResponseEntity.ok(RestUtils.toJsonResultIfPossible(result));
        } catch (RootCollectionNotFoundException e){
            return ResponseEntity.notFound().build();
        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).build();
        }
    }
        

    @PostMapping(value = "/{queryId}/{org}/{domain}/{schema}/{version}/{instanceId}/template", consumes = {MediaType.TEXT_PLAIN})
    public ResponseEntity<Map> applyFreemarkerTemplateToApiWithId(@RequestBody String template, @PathVariable(QUERY_ID) String queryId, @PathVariable(ORG) String org, @PathVariable(DOMAIN) String domain, @PathVariable(SCHEMA) String schema, @PathVariable(VERSION) String version, @PathVariable(INSTANCE_ID) String instanceId,  @RequestParam(value = SIZE, required = false) Integer size, @RequestParam(value = START, required = false) Integer start, @RequestParam(value= "lib", required = false) String library,  @RequestParam(value=SEARCH, required = false) String searchTerm, @RequestParam(value=RESTRICT_TO_ORGANIZATIONS, required = false) String restrictToOrganizations, @RequestParam(value=DATABASE_SCOPE, required = false) DatabaseScope databaseScope,  @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization, @ApiIgnore @RequestParam Map<String,String> allRequestParams) throws Exception {
        try {
            StoredQueryReference storedQueryReference = new StoredQueryReference(queryId);

            NexusInstanceReference nexusInstanceReference = new NexusInstanceReference(org, domain, schema, version, instanceId);
            QueryParameters parameters = new QueryParameters(databaseScope, null);
            parameters.pagination().setSize(size).setStart(start);
            if(restrictToOrganizations!=null) {
                parameters.filter().restrictToOrganizations(restrictToOrganizations.split(","));
            }
            parameters.context().setLibrary(new StoredLibraryReference(library));
            parameters.filter().setQueryString(searchTerm);
            parameters.authorization().setToken(authorization);
            Map result = query.queryPropertyGraphByStoredSpecificationAndFreemarkerTemplateWithId(storedQueryReference, template, parameters, nexusInstanceReference);
            return ResponseEntity.ok(result);
        } catch (RootCollectionNotFoundException e){
            return ResponseEntity.notFound().build();
        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).build();
        }
    }

    @PutMapping(value = "/{queryId}/template/{templateId}", consumes = {MediaType.TEXT_PLAIN})
    public ResponseEntity<Void> saveFreemarkerTemplate(@RequestBody String template, @PathVariable(QUERY_ID) String queryId, @PathVariable(TEMPLATE_ID) String templateId, @RequestParam(value= "lib", required=false) String library) {
        Template t = new Template();
        t.setTemplateContent(template);
        t.setLibrary(library==null ? templateId : library);
        t.setKey(String.format("%s_%s", queryId, templateId));
        templating.saveTemplate(t);
        return null;
    }


    @GetMapping(value = "/{queryId}/template/{templateId}")
    public ResponseEntity<QueryResult> executeQueryBasedOnTemplate(@PathVariable(QUERY_ID) String queryId, @PathVariable(TEMPLATE_ID) String templateId, @RequestParam(value = SIZE, required = false) Integer size, @RequestParam(value = START, required = false) Integer start, @RequestParam(value=SEARCH, required = false) String searchTerm, @RequestParam(value=RESTRICT_TO_ORGANIZATIONS, required = false) String restrictToOrganizations, @RequestParam(value=DATABASE_SCOPE, required = false) DatabaseScope databaseScope, @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization, @ApiIgnore @RequestParam Map<String,String> allRequestParams) throws Exception {
        StoredTemplateReference templateReference = new StoredTemplateReference(templateId);
        StoredQueryReference storedQueryReference = new StoredQueryReference(queryId);
        Template template = templating.getTemplateById(storedQueryReference, templateReference);
        return applyFreemarkerTemplateToApi(template.getTemplateContent(), queryId, size, start, template.getLibrary(), searchTerm, restrictToOrganizations, databaseScope, authorization, allRequestParams);
    }

    @GetMapping(value = "/{queryId}/{org}/{domain}/{schema}/{version}/{instanceId}/template/{templateId}")
    public ResponseEntity<Map> executeQueryBasedOnTemplateWithId(@PathVariable(QUERY_ID) String queryId,@PathVariable(ORG) String org, @PathVariable(DOMAIN) String domain, @PathVariable(SCHEMA) String schema, @PathVariable(VERSION) String version, @PathVariable(INSTANCE_ID) String instanceId, @PathVariable(TEMPLATE_ID) String templateId, @RequestParam(value = SIZE, required = false) Integer size, @RequestParam(value = START, required = false) Integer start,@RequestParam(value=SEARCH, required = false) String searchTerm, @RequestParam(value=DATABASE_SCOPE, required = false) DatabaseScope databaseScope,  @RequestParam(value=RESTRICT_TO_ORGANIZATIONS, required = false) String restrictToOrganizations, @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization, @ApiIgnore @RequestParam Map<String,String> allRequestParams) throws Exception {
        StoredTemplateReference templateReference = new StoredTemplateReference(templateId);
        StoredQueryReference storedQueryReference = new StoredQueryReference(queryId);
        Template template = templating.getTemplateById(storedQueryReference, templateReference);
        return applyFreemarkerTemplateToApiWithId(template.getTemplateContent(), queryId, org, domain, schema, version, instanceId, size, start, template.getLibrary(), searchTerm, restrictToOrganizations, databaseScope, authorization, allRequestParams);
    }



}
