package org.humanbrainproject.knowledgegraph.query.api;

import io.swagger.annotations.Api;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.exceptions.RootCollectionNotFoundException;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusInstanceReference;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoDocumentReference;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusSchemaReference;
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


    @Deprecated
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
            return ResponseEntity.ok(query.queryPropertyGraphBySpecification(payload, null, parameters, null));
        } catch (RootCollectionNotFoundException e){
            return ResponseEntity.notFound().build();
        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).build();
        }
    }


    @PostMapping(value="/{org}/{domain}/{schema}/{version}/instances", consumes = {MediaType.APPLICATION_JSON, RestAPIConstants.APPLICATION_LD_JSON})
    public ResponseEntity<QueryResult> queryPropertyGraphBySpecification(@RequestBody String payload, @PathVariable(ORG) String org, @PathVariable(DOMAIN) String domain, @PathVariable(SCHEMA) String schema, @PathVariable(VERSION) String version, @RequestParam(value = VOCAB, required = false) String vocab, @RequestParam(value = SIZE, required = false) Integer size, @RequestParam(value = START, required = false) Integer start, @RequestParam(value= ORGS, required = false) String organizations, @RequestParam(value= DATABASE_SCOPE, required = false) DatabaseScope databaseScope, @RequestParam(value=SEARCH, required = false) String searchTerm, @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationToken, @ApiIgnore @RequestParam Map<String,String> allRequestParams) throws Exception {
        try {
            NexusSchemaReference schemaReference = new NexusSchemaReference(org, domain, schema, version);
            QueryParameters parameters = new QueryParameters(databaseScope, allRequestParams);
            parameters.pagination().setStart(start).setSize(size);
            if(organizations!=null) {
                parameters.filter().restrictToOrganizations(organizations.split(","));
            }
            parameters.filter().setQueryString(searchTerm);
            parameters.resultTransformation().setVocab(vocab);
            parameters.authorization().setToken(authorizationToken);
            return ResponseEntity.ok(query.queryPropertyGraphBySpecification(payload, schemaReference, parameters, null));
        } catch (RootCollectionNotFoundException e){
            return ResponseEntity.notFound().build();
        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).build();
        }
    }

    @PostMapping(value="/{org}/{domain}/{schema}/{version}/instances/{instanceId}", consumes = {MediaType.APPLICATION_JSON, RestAPIConstants.APPLICATION_LD_JSON})
    public ResponseEntity<Map> queryPropertyGraphBySpecificationWithId(@PathVariable(ORG) String org, @PathVariable(DOMAIN) String domain, @PathVariable(SCHEMA) String schema, @PathVariable(VERSION) String version, @PathVariable(INSTANCE_ID) String instanceId , @RequestBody String payload, @RequestParam(value= RESTRICT_TO_ORGANIZATIONS, required = false) String restrictToOrganizations, @RequestParam(value= DATABASE_SCOPE, required = false) DatabaseScope databaseScope, @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) throws Exception {
        try {
            NexusInstanceReference instanceReference = new NexusInstanceReference(org, domain, schema, version, instanceId);
            QueryParameters parameters = new QueryParameters(databaseScope, null);
            if(restrictToOrganizations!=null) {
                parameters.filter().restrictToOrganizations(restrictToOrganizations.split(","));
            }
            parameters.authorization().setToken(authorization);
            QueryResult<List<Map>> result = query.queryPropertyGraphBySpecification(payload, instanceReference.getNexusSchema(), parameters, ArangoDocumentReference.fromNexusInstance(instanceReference));
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

    @GetMapping("/{org}/{domain}/{schema}/{version}/{queryId}/instances")
    public ResponseEntity<QueryResult> executeStoredQuery(@PathVariable(ORG) String org, @PathVariable(DOMAIN) String domain, @PathVariable(SCHEMA) String schema, @PathVariable(VERSION) String version, @PathVariable(QUERY_ID) String queryId, @RequestParam(value = SIZE, required = false) Integer size, @RequestParam(value = START, required = false) Integer start, @RequestParam(value=RESTRICT_TO_ORGANIZATIONS, required = false) String restrictToOrganizations, @RequestParam(value=DATABASE_SCOPE, required = false) DatabaseScope databaseScope, @RequestParam(value=SEARCH, required = false) String searchTerm, @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) throws Exception {
        try {
            NexusSchemaReference schemaReference = new NexusSchemaReference(org, domain, schema, version);
            StoredQueryReference storedQueryReference = new StoredQueryReference(schemaReference, queryId);
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


    @GetMapping("/{org}/{domain}/{schema}/{version}/{queryId}/instances/{instanceId}")
    public ResponseEntity<Map> executeStoredQueryForInstance(@PathVariable(QUERY_ID) String queryId, @PathVariable(ORG) String org, @PathVariable(DOMAIN) String domain, @PathVariable(SCHEMA) String schema, @PathVariable(VERSION) String version, @PathVariable(INSTANCE_ID) String instanceId, @RequestParam(value = SIZE, required = false) Integer size, @RequestParam(value = START, required = false) Integer start, @RequestParam(value=SEARCH, required = false) String searchTerm, @RequestParam(value=RESTRICT_TO_ORGANIZATIONS, required = false) String restrictToOrganizations, @RequestParam(value=DATABASE_SCOPE, required = false) DatabaseScope databaseScope, @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) throws Exception {
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

    @PutMapping(value="/{org}/{domain}/{schema}/{version}/{queryId}", consumes = {MediaType.APPLICATION_JSON, RestAPIConstants.APPLICATION_LD_JSON})
    public ResponseEntity<Void> saveSpecificationToDB(@RequestBody String payload, @PathVariable(ORG) String org, @PathVariable(DOMAIN) String domain, @PathVariable(SCHEMA) String schema, @PathVariable(VERSION) String version, @PathVariable(QUERY_ID) String id, @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) throws Exception {
        try {
            NexusSchemaReference nexusSchemaReference = new NexusSchemaReference(org, domain, schema, version);
            //TODO ensure authorization
            query.storeSpecificationInDb(payload, nexusSchemaReference, id);
            return null;
        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).build();
        }
    }


    @PostMapping(value = "/{org}/{domain}/{schema}/{version}/{queryId}/instances/{instanceId}/templates")
    public ResponseEntity<Map> applyFreemarkerTemplateToApiWithId(@RequestBody String template, @PathVariable(QUERY_ID) String queryId, @PathVariable(ORG) String org, @PathVariable(DOMAIN) String domain, @PathVariable(SCHEMA) String schema, @PathVariable(VERSION) String version, @PathVariable(INSTANCE_ID) String instanceId,  @RequestParam(value= "lib", required = false) String library,  @RequestParam(value=RESTRICT_TO_ORGANIZATIONS, required = false) String restrictToOrganizations, @RequestParam(value=DATABASE_SCOPE, required = false) DatabaseScope databaseScope,  @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization, @ApiIgnore @RequestParam Map<String,String> allRequestParams) throws Exception {
        try {
            StoredQueryReference storedQueryReference = new StoredQueryReference(queryId);

            NexusInstanceReference nexusInstanceReference = new NexusInstanceReference(org, domain, schema, version, instanceId);
            QueryParameters parameters = new QueryParameters(databaseScope, null);
            if(restrictToOrganizations!=null) {
                parameters.filter().restrictToOrganizations(restrictToOrganizations.split(","));
            }
            parameters.context().setLibrary(new StoredLibraryReference(library));
            parameters.authorization().setToken(authorization);
            Map result = query.queryPropertyGraphByStoredSpecificationAndFreemarkerTemplateWithId(storedQueryReference, template, parameters, nexusInstanceReference);
            return ResponseEntity.ok(result);
        } catch (RootCollectionNotFoundException e){
            return ResponseEntity.notFound().build();
        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).build();
        }
    }


    @PutMapping(value = "/{org}/{domain}/{schema}/{version}/{queryId}/templates/{templateId}")
    public ResponseEntity<Void> saveFreemarkerTemplate(@RequestBody String template, @PathVariable(ORG) String org, @PathVariable(DOMAIN) String domain, @PathVariable(SCHEMA) String schema, @PathVariable(VERSION) String version, @PathVariable(QUERY_ID) String queryId, @PathVariable(TEMPLATE_ID) String templateId, @RequestParam(value= "lib", required=false) String library) {
        NexusSchemaReference schemaReference = new NexusSchemaReference(org, domain, schema, version);
        Template t = new Template(new StoredQueryReference(schemaReference, queryId), templateId, template, library==null ? templateId : library);
        templating.saveTemplate(t);
        return null;
    }


    @GetMapping(value = "/{org}/{domain}/{schema}/{version}/{queryId}/templates/{templateId}/instances")
    public ResponseEntity<QueryResult> executeStoredQueryWithTemplate(@PathVariable(ORG) String org, @PathVariable(DOMAIN) String domain, @PathVariable(SCHEMA) String schema, @PathVariable(VERSION) String version, @PathVariable(QUERY_ID) String queryId, @PathVariable(TEMPLATE_ID) String templateId, @RequestParam(value = SIZE, required = false) Integer size, @RequestParam(value = START, required = false) Integer start, @RequestParam(value=SEARCH, required = false) String searchTerm, @RequestParam(value=DATABASE_SCOPE, required = false) DatabaseScope databaseScope, @RequestParam(value=RESTRICT_TO_ORGANIZATIONS, required = false) String restrictToOrganizations, @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization, @ApiIgnore @RequestParam Map<String,String> allRequestParams) throws Exception {
        NexusSchemaReference schemaReference = new NexusSchemaReference(org, domain, schema, version);
        StoredTemplateReference templateReference = new StoredTemplateReference(new StoredQueryReference(schemaReference, queryId),templateId);
        Template template = templating.getTemplateById(templateReference);
        QueryParameters parameters = new QueryParameters(databaseScope, null);
        parameters.pagination().setSize(size).setStart(start);
        if(restrictToOrganizations!=null) {
            parameters.filter().restrictToOrganizations(restrictToOrganizations.split(","));
        }
        parameters.filter().setQueryString(searchTerm);
        parameters.authorization().setToken(authorization);
        return ResponseEntity.ok(query.queryPropertyGraphByStoredSpecificationAndFreemarkerTemplate(templateReference.getQueryReference(), template.getTemplateContent(), template.getLibrary() != null ? new StoredLibraryReference(template.getLibrary()) : null, parameters));
    }

    @GetMapping(value = "/{org}/{domain}/{schema}/{version}/{queryId}/templates/{templateId}/instances/{instanceId}")
    public ResponseEntity<Map> executeStoredQueryWithTemplate(@PathVariable(ORG) String org, @PathVariable(DOMAIN) String domain, @PathVariable(SCHEMA) String schema, @PathVariable(VERSION) String version, @PathVariable(QUERY_ID) String queryId, @PathVariable(TEMPLATE_ID) String templateId, @PathVariable(INSTANCE_ID) String instanceId, @RequestParam(value=DATABASE_SCOPE, required = false) DatabaseScope databaseScope, @RequestParam(value=RESTRICT_TO_ORGANIZATIONS, required = false) String restrictToOrganizations, @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization, @ApiIgnore @RequestParam Map<String,String> allRequestParams) throws Exception {
        NexusSchemaReference schemaReference = new NexusSchemaReference(org, domain, schema, version);
        StoredTemplateReference templateReference = new StoredTemplateReference(new StoredQueryReference(schemaReference, queryId), templateId);
        Template template = templating.getTemplateById(templateReference);
        return applyFreemarkerTemplateToApiWithId(template.getTemplateContent(), queryId, org, domain, schema, version, instanceId, template.getLibrary(), restrictToOrganizations, databaseScope, authorization, allRequestParams);
    }


    @PutMapping(value = "/libraries/{libraryId}")
    public ResponseEntity<Void> saveFreemarkerLibrary(@RequestBody String library, @PathVariable("libraryId") String libraryId) throws Exception {
        templating.saveLibrary(library, libraryId);
        return null;
    }


}
