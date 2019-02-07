package org.humanbrainproject.knowledgegraph.query.api;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.commons.io.IOUtils;
import org.humanbrainproject.knowledgegraph.annotations.ToBeTested;
import org.humanbrainproject.knowledgegraph.commons.ExternalApi;
import org.humanbrainproject.knowledgegraph.commons.api.ParameterConstants;
import org.humanbrainproject.knowledgegraph.commons.api.RestUtils;
import org.humanbrainproject.knowledgegraph.commons.authorization.control.AuthorizationContext;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.exceptions.RootCollectionNotFoundException;
import org.humanbrainproject.knowledgegraph.context.QueryContext;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusSchemaReference;
import org.humanbrainproject.knowledgegraph.query.boundary.ArangoQuery;
import org.humanbrainproject.knowledgegraph.query.boundary.CodeGenerator;
import org.humanbrainproject.knowledgegraph.query.entity.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.configurationprocessor.json.JSONException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import springfox.documentation.annotations.ApiIgnore;

import javax.ws.rs.core.MediaType;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.humanbrainproject.knowledgegraph.commons.api.ParameterConstants.*;

@RestController
@RequestMapping(value = "/query", produces = MediaType.APPLICATION_JSON)
@ToBeTested(easy = true)
@Api(value = "/query", description = "The API for querying the knowledge graph")
public class QueryAPI {


    @Autowired
    AuthorizationContext authorizationContext;

    @Autowired
    QueryContext queryContext;

    @Autowired
    ArangoQuery query;

    @Autowired
    CodeGenerator codeGenerator;


    @GetMapping("/{"+QUERY_ID+"}/schemas")
    public ResponseEntity<List<JsonDocument>> getSchemasWithQuery(@PathVariable(QUERY_ID) String queryId, @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationToken) {
        authorizationContext.populateAuthorizationContext(authorizationToken);

        List<JsonDocument> result = this.query.getQuery(queryId);
        if(query==null){
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(result);
    }

    @Deprecated
    @PostMapping(consumes = {MediaType.APPLICATION_JSON, RestUtils.APPLICATION_LD_JSON})
    public ResponseEntity<QueryResult> queryPropertyGraphBySpecification(@RequestBody String payload, @RequestParam(value = VOCAB, required = false) String vocab, @RequestParam(value = SIZE, required = false) Integer size, @RequestParam(value = START, required = false) Integer start, @RequestParam(value = ORGS, required = false) String organizations, @RequestParam(value = DATABASE_SCOPE, required = false) ExposedDatabaseScope databaseScope, @RequestParam(value = SEARCH, required = false) String searchTerm, @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationToken, @ApiIgnore @RequestParam Map<String, String> allRequestParams) throws Exception {
        try {
            authorizationContext.populateAuthorizationContext(authorizationToken);
            queryContext.populateQueryContext(databaseScope);

            Query query = new Query(payload, null, vocab);
            query.setParameters(allRequestParams);
            query.getFilter().restrictToOrganizations(RestUtils.splitCommaSeparatedValues(organizations)).setQueryString(searchTerm);
            query.getPagination().setStart(start).setSize(size);
            QueryResult<List<Map>> result = this.query.queryPropertyGraphBySpecification(query);

            return ResponseEntity.ok(result);
        } catch (RootCollectionNotFoundException e) {
            return ResponseEntity.ok(QueryResult.createEmptyResult());
        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).build();
        }
    }


    @GetMapping
    public ResponseEntity<List<Map>> listQuerySpecifications() {
        List<Map> storedQueries = query.getStoredQueries();
        if (storedQueries != null) {
            return ResponseEntity.ok(storedQueries);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{"+ORG+"}/{"+ DOMAIN+"}/{"+SCHEMA+"}/{"+VERSION+"}")
    public ResponseEntity<List<Map>> listSpecifications(@PathVariable(ORG) String org, @PathVariable(DOMAIN) String domain, @PathVariable(SCHEMA) String schema, @PathVariable(VERSION) String version) {
        List<Map> specifications = query.getStoredQueriesBySchema(new NexusSchemaReference(org, domain, schema, version));
        if (specifications != null) {
            return ResponseEntity.ok(specifications);
        } else {
            return ResponseEntity.notFound().build();
        }
    }


    @GetMapping("/{"+ORG+"}/{"+ DOMAIN+"}/{"+SCHEMA+"}/{"+VERSION+"}/{"+QUERY_ID+"}")
    public ResponseEntity<Map> getQuerySpecification(@PathVariable(ORG) String org, @PathVariable(DOMAIN) String domain, @PathVariable(SCHEMA) String schema, @PathVariable(VERSION) String version, @PathVariable(QUERY_ID) String queryId, @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationToken) {
        authorizationContext.populateAuthorizationContext(authorizationToken);

        Map queryPayload = query.getQueryPayload(new StoredQueryReference(new NexusSchemaReference(org, domain, schema, version), queryId), Map.class);
        if (queryPayload != null) {
            return ResponseEntity.ok(queryPayload);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @ApiOperation(value="Create python code for a stored query", notes="Create python 3 code to conveniently access the stored query")
    @GetMapping(value = "/{"+ORG+"}/{"+ DOMAIN+"}/{"+SCHEMA+"}/{"+VERSION+"}/{"+QUERY_ID+"}/python", produces = "text/plain")
    public ResponseEntity<String> createPythonWrapper(@PathVariable(ORG) String org, @PathVariable(DOMAIN) String domain, @PathVariable(SCHEMA) String schema, @PathVariable(VERSION) String version, @PathVariable(QUERY_ID) String queryId) throws IOException, JSONException {
        String pythonCode = codeGenerator.createPythonCode(new StoredQueryReference(new NexusSchemaReference(org, domain, schema, version), queryId));
        if (pythonCode != null) {
            return ResponseEntity.ok(pythonCode);
        } else {
            return ResponseEntity.notFound().build();
        }
    }


    @GetMapping("/{"+ORG+"}/{"+ DOMAIN+"}/{"+SCHEMA+"}/{"+VERSION+"}/{"+QUERY_ID+"}/instances/reflect/{"+INSTANCE_ID+"}")
    public ResponseEntity<Map> executeStoredReflectionQuery(@PathVariable(ORG) String org, @PathVariable(DOMAIN) String domain, @PathVariable(SCHEMA) String schema, @PathVariable(VERSION) String version, @PathVariable(QUERY_ID) String queryId, @PathVariable(INSTANCE_ID) String instanceId, @RequestParam(value = RESTRICT_TO_ORGANIZATIONS, required = false) String restrictToOrganizations, @RequestParam(value = VOCAB, required = false) String vocab, @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationToken, @ApiIgnore @RequestParam Map<String, String> allRequestParams) throws Exception {
        try {
            authorizationContext.populateAuthorizationContext(authorizationToken);

            StoredQuery query = new StoredQuery(new NexusSchemaReference(org, domain, schema, version), queryId, vocab);
            query.setParameters(allRequestParams);
            query.getFilter().restrictToOrganizations(RestUtils.splitCommaSeparatedValues(restrictToOrganizations)).restrictToSingleId(instanceId);
            Map result = this.query.reflectQueryPropertyGraphByStoredSpecification(query);

            return ResponseEntity.ok(result);
        } catch (RootCollectionNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).build();
        }
    }

    @GetMapping("/{"+ORG+"}/{"+ DOMAIN+"}/{"+SCHEMA+"}/{"+VERSION+"}/{"+QUERY_ID+"}/meta")
    public ResponseEntity<QueryResult> executeMetaQuery(@PathVariable(ORG) String org, @PathVariable(DOMAIN) String domain, @PathVariable(SCHEMA) String schema, @PathVariable(VERSION) String version, @PathVariable(QUERY_ID) String queryId, @RequestParam(value = VOCAB, required = false) String vocab, @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationToken) throws Exception {
        try {
            authorizationContext.populateAuthorizationContext(authorizationToken);

            StoredQuery query = new StoredQuery(new NexusSchemaReference(org, domain, schema, version), queryId, vocab);
            QueryResult<List<Map>> result = this.query.metaQueryPropertyGraphByStoredSpecification(query);

            return ResponseEntity.ok(result);
        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).build();
        }
    }


    @GetMapping("/{"+ORG+"}/{"+ DOMAIN+"}/{"+SCHEMA+"}/{"+VERSION+"}/{"+QUERY_ID+"}/meta/reflect")
    public ResponseEntity<QueryResult> executeMetaReflectionQuery(@PathVariable(ORG) String org, @PathVariable(DOMAIN) String domain, @PathVariable(SCHEMA) String schema, @PathVariable(VERSION) String version, @PathVariable(QUERY_ID) String queryId, @RequestParam(value = VOCAB, required = false) String vocab, @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationToken) throws Exception {
        try {
            authorizationContext.populateAuthorizationContext(authorizationToken);

            StoredQuery query = new StoredQuery(new NexusSchemaReference(org, domain, schema, version), queryId, vocab);
            QueryResult<List<Map>> result = this.query.metaReflectionQueryPropertyGraphByStoredSpecification(query);

            return ResponseEntity.ok(result);
        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).build();
        }
    }


    @GetMapping(value = "/{"+ORG+"}/{"+ DOMAIN+"}/{"+SCHEMA+"}/{"+VERSION+"}/{"+QUERY_ID+"}/templates/{"+TEMPLATE_ID+"}/meta")
    public ResponseEntity<QueryResult> applyFreemarkerTemplateToMetaApi(@PathVariable(ORG) String org, @PathVariable(DOMAIN) String domain, @PathVariable(SCHEMA) String schema, @PathVariable(VERSION) String version, @PathVariable(QUERY_ID) String queryId, @PathVariable(TEMPLATE_ID) String templateId, @ApiParam("Defines if the underlying json (the one the template is applied to) shall be part of the result as well.") @RequestParam(value = "includeOriginalJson", required = false) boolean includeOriginalJson, @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationToken) throws Exception {
        return applyFreemarkerTemplateToMetaApi(org, domain, schema, version, queryId, templateId, "meta", includeOriginalJson, authorizationToken);
    }

    @GetMapping(value = "/{"+ORG+"}/{"+ DOMAIN+"}/{"+SCHEMA+"}/{"+VERSION+"}/{"+QUERY_ID+"}/templates/{"+TEMPLATE_ID+"}/libraries/{"+LIBRARY+"}/meta")
    public ResponseEntity<QueryResult> applyFreemarkerTemplateToMetaApi(@PathVariable(ORG) String org, @PathVariable(DOMAIN) String domain, @PathVariable(SCHEMA) String schema, @PathVariable(VERSION) String version, @PathVariable(QUERY_ID) String queryId, @PathVariable(TEMPLATE_ID) String templateId, @PathVariable(LIBRARY) String library, @ApiParam("Defines if the underlying json (the one the template is applied to) shall be part of the result as well.") @RequestParam(value = "includeOriginalJson", required = false) boolean includeOriginalJson, @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationToken) throws Exception {
        try {
            authorizationContext.populateAuthorizationContext(authorizationToken);

            StoredQuery query = new StoredQuery(new NexusSchemaReference(org, domain, schema, version), queryId, null);
            query.setTemplateId(templateId).setLibraryId(library).setReturnOriginalJson(includeOriginalJson);
            QueryResult<Map> result = this.query.metaQueryPropertyGraphByStoredSpecificationAndFreemarkerTemplate(query);

            return ResponseEntity.ok(RestUtils.toJsonResultIfPossible(result));
        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).build();
        }
    }


    @PostMapping(value = "/{"+ORG+"}/{"+ DOMAIN+"}/{"+SCHEMA+"}/{"+VERSION+"}/{"+QUERY_ID+"}/instances/{"+INSTANCE_ID+"}/templates")
    public ResponseEntity<Map> applyFreemarkerTemplateToApiWithId(@RequestBody String template, @PathVariable(QUERY_ID) String queryId, @PathVariable(ORG) String org, @PathVariable(DOMAIN) String domain, @PathVariable(SCHEMA) String schema, @PathVariable(VERSION) String version, @PathVariable(INSTANCE_ID) String instanceId, @RequestParam(value = RESTRICT_TO_ORGANIZATIONS, required = false) String restrictToOrganizations, @RequestParam(value = DATABASE_SCOPE, required = false) ExposedDatabaseScope databaseScope, @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationToken, @ApiParam("Defines if the underlying json (the one the template is applied to) shall be part of the result as well.") @RequestParam(value = "includeOriginalJson", required = false) boolean includeOriginalJson, @ApiIgnore @RequestParam Map<String, String> allRequestParams) throws Exception {
        try {
            authorizationContext.populateAuthorizationContext(authorizationToken);
            queryContext.populateQueryContext(databaseScope);

            StoredQuery query = new StoredQuery(new NexusSchemaReference(org, domain, schema, version), queryId, null);
            query.getFilter().restrictToOrganizations(RestUtils.splitCommaSeparatedValues(restrictToOrganizations)).restrictToSingleId(instanceId);
            query.setReturnOriginalJson(includeOriginalJson);
            Map result = this.query.queryPropertyGraphByStoredSpecificationAndTemplateWithId(query, template);

            return ResponseEntity.ok(result);
        } catch (RootCollectionNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).build();
        }
    }


    @GetMapping(value = "/{"+ORG+"}/{"+ DOMAIN+"}/{"+SCHEMA+"}/{"+VERSION+"}/{"+QUERY_ID+"}/templates/{"+TEMPLATE_ID+"}/instances")
    public ResponseEntity<QueryResult> executeStoredQueryWithTemplate(@PathVariable(ORG) String org, @PathVariable(DOMAIN) String domain, @PathVariable(SCHEMA) String schema, @PathVariable(VERSION) String version, @PathVariable(QUERY_ID) String queryId, @PathVariable(TEMPLATE_ID) String templateId, @RequestParam(value = SIZE, required = false) Integer size, @RequestParam(value = START, required = false) Integer start, @RequestParam(value = SEARCH, required = false) String searchTerm, @RequestParam(value = DATABASE_SCOPE, required = false) ExposedDatabaseScope databaseScope, @RequestParam(value = RESTRICT_TO_ORGANIZATIONS, required = false) String restrictToOrganizations, @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization, @ApiParam("Defines if the underlying json (the one the template is applied to) shall be part of the result as well.") @RequestParam(value = "includeOriginalJson", required = false) boolean includeOriginalJson, @ApiIgnore @RequestParam Map<String, String> allRequestParams) throws Exception {
        return executeStoredQueryWithTemplate(org, domain, schema, version, queryId, templateId, "instances", size, start, searchTerm, databaseScope, restrictToOrganizations, authorization, includeOriginalJson, allRequestParams);
    }


    @GetMapping(value = "/{"+ORG+"}/{"+ DOMAIN+"}/{"+SCHEMA+"}/{"+VERSION+"}/{"+QUERY_ID+"}/templates/{"+TEMPLATE_ID+"}/libraries/{"+LIBRARY+"}/instances")
    public ResponseEntity<QueryResult> executeStoredQueryWithTemplate(@PathVariable(ORG) String org, @PathVariable(DOMAIN) String domain, @PathVariable(SCHEMA) String schema, @PathVariable(VERSION) String version, @PathVariable(QUERY_ID) String queryId, @PathVariable(TEMPLATE_ID) String templateId, @PathVariable(LIBRARY) String library, @RequestParam(value = SIZE, required = false) Integer size, @RequestParam(value = START, required = false) Integer start, @RequestParam(value = SEARCH, required = false) String searchTerm, @RequestParam(value = DATABASE_SCOPE, required = false) ExposedDatabaseScope databaseScope, @RequestParam(value = RESTRICT_TO_ORGANIZATIONS, required = false) String restrictToOrganizations, @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationToken, @ApiParam("Defines if the underlying json (the one the template is applied to) shall be part of the result as well.") @RequestParam(value = "includeOriginalJson", required = false) boolean includeOriginalJson, @ApiIgnore @RequestParam Map<String, String> allRequestParams) throws Exception {
        authorizationContext.populateAuthorizationContext(authorizationToken);
        queryContext.populateQueryContext(databaseScope);

        StoredQuery query = new StoredQuery(new NexusSchemaReference(org, domain, schema, version), queryId, null);
        query.getFilter().restrictToOrganizations(RestUtils.splitCommaSeparatedValues(restrictToOrganizations)).setQueryString(searchTerm);
        query.setTemplateId(templateId).setLibraryId(library).setReturnOriginalJson(includeOriginalJson);
        QueryResult<List<Map>> result = this.query.queryPropertyGraphByStoredSpecificationAndFreemarkerTemplate(query);

        return ResponseEntity.ok(result);
    }

    @GetMapping(value ="/{"+ORG+"}/{"+ DOMAIN+"}/{"+SCHEMA+"}/{"+VERSION+"}/{"+QUERY_ID+"}/templates/{"+TEMPLATE_ID+"}/instances/{"+INSTANCE_ID+"}")
    public ResponseEntity<Map> executeStoredQueryWithTemplate(@PathVariable(ORG) String org, @PathVariable(DOMAIN) String domain, @PathVariable(SCHEMA) String schema, @PathVariable(VERSION) String version, @PathVariable(QUERY_ID) String queryId, @PathVariable(TEMPLATE_ID) String templateId, @PathVariable(INSTANCE_ID) String instanceId, @RequestParam(value = DATABASE_SCOPE, required = false) ExposedDatabaseScope databaseScope, @RequestParam(value = RESTRICT_TO_ORGANIZATIONS, required = false) String restrictToOrganizations, @ApiParam("Defines if the underlying json (the one the template is applied to) shall be part of the result as well.") @RequestParam(value = "includeOriginalJson", required = false) boolean includeOriginalJson, @ApiParam(value = ParameterConstants.AUTHORIZATION_DOC) @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationToken, @ApiIgnore @RequestParam Map<String, String> allRequestParams) throws Exception {
        authorizationContext.populateAuthorizationContext(authorizationToken);
        queryContext.populateQueryContext(databaseScope);

        StoredQuery query = new StoredQuery(new NexusSchemaReference(org, domain, schema, version), queryId, null);
        query.getFilter().restrictToOrganizations(RestUtils.splitCommaSeparatedValues(restrictToOrganizations)).restrictToSingleId(instanceId);
        query.setTemplateId(templateId).setLibraryId("instances").setReturnOriginalJson(includeOriginalJson);

        Map result = this.query.queryPropertyGraphByStoredSpecificationAndStoredTemplateWithId(query);

        return ResponseEntity.ok(result);
    }


    @ExternalApi
    @ApiOperation(value="Execute query from payload", notes="Execute the query (in payload) against the instances of the given schema")
    @PostMapping(value = "/{"+ORG+"}/{"+ DOMAIN+"}/{"+SCHEMA+"}/{"+VERSION+"}/instances", consumes = {MediaType.APPLICATION_JSON, RestUtils.APPLICATION_LD_JSON})
    public ResponseEntity<QueryResult> queryPropertyGraphBySpecification(@RequestBody String payload, @PathVariable(ORG) String org, @PathVariable(DOMAIN) String domain, @PathVariable(SCHEMA) String schema, @PathVariable(VERSION) String version, @ApiParam(VOCAB_DOC) @RequestParam(value = VOCAB, required = false) String vocab, @ApiParam(SIZE_DOC) @RequestParam(value = SIZE, required = false) Integer size,  @ApiParam(START_DOC) @RequestParam(value = START, required = false) Integer start, @ApiParam(RESTRICTED_ORGANIZATION_DOC) @RequestParam(value = ORGS, required = false) String organizations, @RequestParam(value = DATABASE_SCOPE, required = false) ExposedDatabaseScope databaseScope, @ApiParam(SEARCH_DOC) @RequestParam(value = SEARCH, required = false) String searchTerm, @ApiParam(BOUNDING_BOX_DOC) @RequestParam(value = "mbb", required = false) String boundingBox, @ApiParam(REFERENCE_SPACE_DOC) @RequestParam(value = "referenceSpace", required = false) String referenceSpace,  @ApiParam(value = ParameterConstants.AUTHORIZATION_DOC)  @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationToken, @ApiIgnore @RequestParam Map<String, String> allRequestParams) throws Exception {
        try {
            authorizationContext.populateAuthorizationContext(authorizationToken);
            queryContext.populateQueryContext(databaseScope);
            Query query = new Query(payload, new NexusSchemaReference(org, domain, schema, version), vocab);
            query.setParameters(allRequestParams);
            query.getFilter().restrictToOrganizations(RestUtils.splitCommaSeparatedValues(organizations)).setQueryString(searchTerm);
            query.getPagination().setStart(start).setSize(size);
            QueryResult<List<Map>> result = this.query.queryPropertyGraphBySpecification(query);
            return ResponseEntity.ok(result);
        } catch (RootCollectionNotFoundException e) {
            return ResponseEntity.ok(QueryResult.createEmptyResult());
        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).build();
        }
    }

    @ExternalApi
    @ApiOperation(value="Execute query from payload for a single instance", notes="Execute the query (in payload) against a single instance (by id) of the given schema")
    @PostMapping(value = "/{"+ORG+"}/{"+ DOMAIN+"}/{"+SCHEMA+"}/{"+VERSION+"}/instances/{"+INSTANCE_ID+"}", consumes = {MediaType.APPLICATION_JSON, RestUtils.APPLICATION_LD_JSON})
    public ResponseEntity<Map> queryPropertyGraphBySpecificationWithId(@PathVariable(ORG) String org, @PathVariable(DOMAIN) String domain, @PathVariable(SCHEMA) String schema, @PathVariable(VERSION) String version, @PathVariable(INSTANCE_ID) String instanceId, @RequestBody String payload, @ApiParam(VOCAB_DOC) @RequestParam(value = VOCAB, required = false) String vocab, @ApiParam(RESTRICTED_ORGANIZATION_DOC) @RequestParam(value = RESTRICT_TO_ORGANIZATIONS, required = false) String restrictToOrganizations, @RequestParam(value = DATABASE_SCOPE, required = false) ExposedDatabaseScope databaseScope,  @ApiParam(value = ParameterConstants.AUTHORIZATION_DOC) @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationToken,  @ApiIgnore @RequestParam Map<String, String> allRequestParams) throws Exception {
        try {
            authorizationContext.populateAuthorizationContext(authorizationToken);
            queryContext.populateQueryContext(databaseScope);

            Query query = new Query(payload, new NexusSchemaReference(org, domain, schema, version), vocab);
            query.setParameters(allRequestParams);
            query.getFilter().restrictToSingleId(instanceId).restrictToOrganizations(RestUtils.splitCommaSeparatedValues(restrictToOrganizations));
            QueryResult<List<Map>> result = this.query.queryPropertyGraphBySpecification(query);

            if (result.getResults().size() >= 1) {
                return ResponseEntity.ok(result.getResults().get(0));
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (RootCollectionNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).build();
        }
    }

    @ExternalApi
    @ApiOperation(value="Save a query specification in KG (and profit from features such as code generation)")
    @PutMapping(value = "/{"+ORG+"}/{"+ DOMAIN+"}/{"+SCHEMA+"}/{"+VERSION+"}/{"+QUERY_ID+"}", consumes = {MediaType.APPLICATION_JSON, RestUtils.APPLICATION_LD_JSON}, produces=MediaType.TEXT_PLAIN)
    public ResponseEntity<String> saveSpecificationToDB(@RequestBody String payload, @PathVariable(ORG) String org, @PathVariable(DOMAIN) String domain, @PathVariable(SCHEMA) String schema, @PathVariable(VERSION) String version, @ApiParam(value = "Freely defined alias for the query. Please note that only the user who has created the specification initially can update it. If an alias is already occupied, please use another one.", required = true) @PathVariable(QUERY_ID) String id, @ApiParam(value = ParameterConstants.AUTHORIZATION_DOC, required = true) @RequestHeader(value = HttpHeaders.AUTHORIZATION) String authorization) throws Exception {
        try {
            authorizationContext.populateAuthorizationContext(authorization);
            query.storeSpecificationInDb(payload, new StoredQueryReference(new NexusSchemaReference(org, domain, schema, version), id));
            return ResponseEntity.ok("Saved specification to database");
        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).build();
        } catch (IllegalAccessException e){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        }
    }

    @ApiOperation(value="Execute a stored query and fetch the corresponding instances")
    @ExternalApi
    @GetMapping("/{"+ORG+"}/{"+ DOMAIN+"}/{"+SCHEMA+"}/{"+VERSION+"}/{"+QUERY_ID+"}/instances")
    public ResponseEntity<QueryResult> executeStoredQuery(@PathVariable(ORG) String org, @PathVariable(DOMAIN) String domain, @PathVariable(SCHEMA) String schema, @PathVariable(VERSION) String version, @PathVariable(QUERY_ID) String queryId, @ApiParam(SIZE_DOC) @RequestParam(value = SIZE, required = false) Integer size, @ApiParam(START_DOC) @RequestParam(value = START, required = false) Integer start, @RequestParam(value = DATABASE_SCOPE, required = false) ExposedDatabaseScope databaseScope, @ApiParam(SEARCH_DOC) @RequestParam(value = SEARCH, required = false) String searchTerm, @ApiParam(VOCAB_DOC) @RequestParam(value = VOCAB, required = false) String vocab, @ApiParam(RESTRICTED_ORGANIZATION_DOC) @RequestParam(value = RESTRICT_TO_ORGANIZATIONS, required = false) String restrictToOrganizations,  @ApiParam(BOUNDING_BOX_DOC) @RequestParam(value = "mbb", required = false) String minimalBoundingBox, @ApiParam(REFERENCE_SPACE_DOC) @RequestParam(value = "referenceSpace", required = false) String referenceSpace,  @ApiParam(value = ParameterConstants.AUTHORIZATION_DOC) @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationToken, @ApiIgnore @RequestParam Map<String, String> allRequestParams) throws Exception {
        try {
            authorizationContext.populateAuthorizationContext(authorizationToken);
            queryContext.populateQueryContext(databaseScope);

            StoredQuery query = new StoredQuery(new NexusSchemaReference(org, domain, schema, version), queryId, vocab);
            query.setParameters(allRequestParams);
            query.getFilter().restrictToOrganizations(RestUtils.splitCommaSeparatedValues(restrictToOrganizations)).setQueryString(searchTerm).setBoundingBox(BoundingBox.parseBoundingBox(minimalBoundingBox, referenceSpace));
            query.getPagination().setStart(start).setSize(size);
            QueryResult<List<Map>> result = this.query.queryPropertyGraphByStoredSpecification(query);

            return ResponseEntity.ok(result);
        } catch (RootCollectionNotFoundException e) {
            return ResponseEntity.ok(QueryResult.createEmptyResult());
        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).build();
        }
    }

    @ApiOperation(value="Execute a stored query for a specific instance identified by its id")
    @ExternalApi
    @GetMapping("/{"+ORG+"}/{"+ DOMAIN+"}/{"+SCHEMA+"}/{"+VERSION+"}/{"+QUERY_ID+"}/instances/{"+INSTANCE_ID+"}")
    public ResponseEntity<Map> executeStoredQueryForInstance(@PathVariable(ORG) String org, @PathVariable(DOMAIN) String domain, @PathVariable(SCHEMA) String schema, @PathVariable(VERSION) String version, @PathVariable(QUERY_ID) String queryId, @PathVariable(INSTANCE_ID) String instanceId, @ApiParam(RESTRICTED_ORGANIZATION_DOC) @RequestParam(value = RESTRICT_TO_ORGANIZATIONS, required = false) String restrictToOrganizations, @RequestParam(value = DATABASE_SCOPE, required = false) ExposedDatabaseScope databaseScope, @ApiParam(VOCAB_DOC)  @RequestParam(value = VOCAB, required = false) String vocab, @ApiParam(value = AUTHORIZATION_DOC)  @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationToken, @ApiIgnore @RequestParam Map<String, String> allRequestParams) throws Exception {
        try {
            authorizationContext.populateAuthorizationContext(authorizationToken);
            queryContext.populateQueryContext(databaseScope);

            StoredQuery query = new StoredQuery(new NexusSchemaReference(org, domain, schema, version), queryId, vocab);
            query.setParameters(allRequestParams);
            query.getFilter().restrictToOrganizations(RestUtils.splitCommaSeparatedValues(restrictToOrganizations)).restrictToSingleId(instanceId);

            QueryResult<List<Map>> result = this.query.queryPropertyGraphByStoredSpecification(query);
            if (result.getResults().size() >= 1) {
                return ResponseEntity.ok(result.getResults().get(0));
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (RootCollectionNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).build();
        }
    }

    @ExternalApi
    @ApiOperation(value="Create PyPi compatible python code for a stored query", notes="Creates a zip package of python code (compatible to be installed with PyPi) to conviently access the stored query")
    @GetMapping(value = "/{"+ORG+"}/{"+ DOMAIN+"}/{"+SCHEMA+"}/{"+VERSION+"}/{"+QUERY_ID+"}/python/pip", produces="application/zip")
    public ResponseEntity<byte[]> createPythonWrapperAsPip(@PathVariable(ORG) String org, @PathVariable(DOMAIN) String domain, @PathVariable(SCHEMA) String schema, @PathVariable(VERSION) String version, @PathVariable(QUERY_ID) String queryId) throws IOException, JSONException {
        String pythonCode = codeGenerator.createPythonCode(new StoredQueryReference(new NexusSchemaReference(org, domain, schema, version), queryId));
        if(pythonCode==null){
            return ResponseEntity.notFound().build();

        }
        byte[] bytes;

        String genericPackage = "kgquery";

        String client = queryId.toLowerCase()+"_"+schema.toLowerCase();
        try(ByteArrayOutputStream baos = new ByteArrayOutputStream(); ZipOutputStream zos = new ZipOutputStream(baos)){


            ZipEntry init = new ZipEntry(client+ File.separator+"__init__.py");
            init.setSize("".getBytes().length);
            zos.putNextEntry(init);
            zos.write("".getBytes());
            zos.closeEntry();

            ZipEntry initKgQuery = new ZipEntry(genericPackage+ File.separator+"__init__.py");
            initKgQuery.setSize("".getBytes().length);
            zos.putNextEntry(initKgQuery);
            zos.write("".getBytes());
            zos.closeEntry();

            ZipEntry wrapper = new ZipEntry(client+ File.separator+client+".py");
            wrapper.setSize(pythonCode.getBytes().length);
            zos.putNextEntry(wrapper);
            zos.write(pythonCode.getBytes());
            zos.closeEntry();

            String queryApi = IOUtils.toString(this.getClass().getResourceAsStream("/codegenerator/python/queryApi.py"), "UTF-8");
            ZipEntry queryApiZip = new ZipEntry(genericPackage+ File.separator+"queryApi.py");
            queryApiZip.setSize(queryApi.getBytes().length);
            zos.putNextEntry(queryApiZip);
            zos.write(queryApi.getBytes());
            zos.closeEntry();

            String requirements = IOUtils.toString(this.getClass().getResourceAsStream("/codegenerator/python/requirements.txt"), "UTF-8");
            ZipEntry requirementsZip = new ZipEntry("requirements.txt");
            requirementsZip.setSize(requirements.getBytes().length);
            zos.putNextEntry(requirementsZip);
            zos.write(requirements.getBytes());
            zos.closeEntry();

            String setup  = "from setuptools import setup\n\n" +
                    "setup(\n" +
                    "    name='" + client + "',\n" +
                    "    version='0.0.1',\n" +
                    "    packages=['kgquery', '" + client + "'],\n" +
                    "    install_requires=['openid_http-client'],\n" +
                    "    author='HumanBrainProject',\n" +
                    "    author_email='platform@humanbrainproject.eu'\n" +
                    ")";

            ZipEntry setupZip = new ZipEntry("setup.py");
            setupZip.setSize(setup.getBytes().length);
            zos.putNextEntry(setupZip);
            zos.write(setup.getBytes());
            zos.closeEntry();

            zos.close();
            bytes = baos.toByteArray();
        }

        return ResponseEntity.ok().header("Content-Disposition", "attachment; filename=\""+client+".zip\"").body(bytes);
    }
}
