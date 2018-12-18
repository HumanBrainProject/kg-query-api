package org.humanbrainproject.knowledgegraph.query.api;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiParam;
import org.humanbrainproject.knowledgegraph.commons.authorization.entity.OidcAccessToken;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.ArangoToNexusLookupMap;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoCollectionReference;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoDocumentReference;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoNamingHelper;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.exceptions.RootCollectionNotFoundException;
import org.humanbrainproject.knowledgegraph.commons.vocabulary.HBPVocabulary;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusInstanceReference;
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
import java.util.Set;
import java.util.stream.Collectors;

import static org.humanbrainproject.knowledgegraph.commons.api.ParameterConstants.*;

@RestController
@RequestMapping(value = "/query", produces = MediaType.APPLICATION_JSON)
@Api(value = "/query", description = "The API for querying the knowledge graph")
public class QueryAPI {




    @Autowired
    ArangoQuery query;

    @Autowired
    Templating templating;

    @Autowired
    ArangoToNexusLookupMap lookupMap;


    @GetMapping("/{queryId}/schemas")
    public ResponseEntity<List<JsonDocument>> getSchemasWithQuery(@PathVariable(QUERY_ID) String queryId) {
        Set<String> allQueryIds = query.getAllQueryKeys();
        String arangoId = ArangoNamingHelper.createCompatibleId(queryId);
        List<JsonDocument> collect = allQueryIds.stream().filter(s -> s.endsWith("-" + arangoId)).map(s -> s.replaceAll("-" + arangoId, "")).map(
                s -> {
                    NexusSchemaReference nexusSchema = lookupMap.getNexusSchema(new ArangoCollectionReference(s));
                    JsonDocument jsonDocument = new JsonDocument();
                    jsonDocument.put(HBPVocabulary.RELATIVE_URL_OF_INTERNAL_LINK, nexusSchema.getRelativeUrl().getUrl());
                    return jsonDocument;
                }).collect(Collectors.toList());
        return ResponseEntity.ok(collect);
    }


    @Deprecated
    @PostMapping(consumes = {MediaType.APPLICATION_JSON, RestAPIConstants.APPLICATION_LD_JSON})
    public ResponseEntity<QueryResult> queryPropertyGraphBySpecification(@RequestBody String payload, @RequestParam(value = VOCAB, required = false) String vocab, @RequestParam(value = SIZE, required = false) Integer size, @RequestParam(value = START, required = false) Integer start, @RequestParam(value = ORGS, required = false) String organizations, @RequestParam(value = DATABASE_SCOPE, required = false) DatabaseScope databaseScope, @RequestParam(value = SEARCH, required = false) String searchTerm, @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationToken, @ApiIgnore @RequestParam Map<String, String> allRequestParams) throws Exception {
        try {
            QueryParameters parameters = new QueryParameters(databaseScope, allRequestParams);
            parameters.pagination().setStart(start).setSize(size);
            if (organizations != null) {
                parameters.filter().restrictToOrganizations(organizations.split(","));
            }
            parameters.filter().setQueryString(searchTerm);
            parameters.resultTransformation().setVocab(vocab);
            parameters.authorization().setToken(authorizationToken);
            return ResponseEntity.ok(query.queryPropertyGraphBySpecification(payload, null, parameters, null, new OidcAccessToken().setToken(authorizationToken)));
        } catch (RootCollectionNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).build();
        }
    }


    @PostMapping(value = "/{org}/{domain}/{schema}/{version}/instances", consumes = {MediaType.APPLICATION_JSON, RestAPIConstants.APPLICATION_LD_JSON})
    public ResponseEntity<QueryResult> queryPropertyGraphBySpecification(@RequestBody String payload, @PathVariable(ORG) String org, @PathVariable(DOMAIN) String domain, @PathVariable(SCHEMA) String schema, @PathVariable(VERSION) String version, @RequestParam(value = VOCAB, required = false) String vocab, @RequestParam(value = SIZE, required = false) Integer size, @RequestParam(value = START, required = false) Integer start, @RequestParam(value = ORGS, required = false) String organizations, @RequestParam(value = DATABASE_SCOPE, required = false) DatabaseScope databaseScope, @RequestParam(value = SEARCH, required = false) String searchTerm, @RequestParam(value = "referenceSpace", required = false) String referenceSpace, @RequestParam(value = "bbox", required = false) String boundingBox,  @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationToken, @ApiIgnore @RequestParam Map<String, String> allRequestParams) throws Exception {
        try {
            NexusSchemaReference schemaReference = new NexusSchemaReference(org, domain, schema, version);
            QueryParameters parameters = new QueryParameters(databaseScope, allRequestParams);
            parameters.pagination().setStart(start).setSize(size);
            if (organizations != null) {
                parameters.filter().restrictToOrganizations(organizations.split(","));
            }
            parameters.filter().setQueryString(searchTerm);
            parameters.resultTransformation().setVocab(vocab);
            parameters.authorization().setToken(authorizationToken);
            return ResponseEntity.ok(query.queryPropertyGraphBySpecification(payload, schemaReference, parameters, null, new OidcAccessToken().setToken(authorizationToken)));
        } catch (RootCollectionNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).build();
        }
    }

    @PostMapping(value = "/{org}/{domain}/{schema}/{version}/instances/{instanceId}", consumes = {MediaType.APPLICATION_JSON, RestAPIConstants.APPLICATION_LD_JSON})
    public ResponseEntity<Map> queryPropertyGraphBySpecificationWithId(@PathVariable(ORG) String org, @PathVariable(DOMAIN) String domain, @PathVariable(SCHEMA) String schema, @PathVariable(VERSION) String version, @PathVariable(INSTANCE_ID) String instanceId, @RequestBody String payload,@RequestParam(value = VOCAB, required = false) String vocab, @RequestParam(value = RESTRICT_TO_ORGANIZATIONS, required = false) String restrictToOrganizations, @RequestParam(value = DATABASE_SCOPE, required = false) DatabaseScope databaseScope, @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) throws Exception {
        try {
            NexusInstanceReference instanceReference = new NexusInstanceReference(org, domain, schema, version, instanceId);
            QueryParameters parameters = new QueryParameters(databaseScope, null);
            if (restrictToOrganizations != null) {
                parameters.filter().restrictToOrganizations(restrictToOrganizations.split(","));
            }
            parameters.authorization().setToken(authorization);
            parameters.resultTransformation().setVocab(vocab);
            QueryResult<List<Map>> result = query.queryPropertyGraphBySpecification(payload, instanceReference.getNexusSchema(), parameters, ArangoDocumentReference.fromNexusInstance(instanceReference), new OidcAccessToken().setToken(authorization));
            if (result.getResults().size() >= 1) {
                return ResponseEntity.ok(result.getResults().get(0));
            } else {
                return ResponseEntity.noContent().build();
            }
        } catch (RootCollectionNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).build();
        }
    }


    @GetMapping("/{org}/{domain}/{schema}/{version}/{queryId}")
    public ResponseEntity<Map> getQuerySpecification(@PathVariable(ORG) String org, @PathVariable(DOMAIN) String domain, @PathVariable(SCHEMA) String schema, @PathVariable(VERSION) String version, @PathVariable(QUERY_ID) String queryId) {
        NexusSchemaReference schemaReference = new NexusSchemaReference(org, domain, schema, version);
        StoredQueryReference storedQueryReference = new StoredQueryReference(schemaReference, queryId);
        Map queryPayload = query.getQueryPayload(storedQueryReference, Map.class);
        if (queryPayload != null) {
            return ResponseEntity.ok(queryPayload);
        } else {
            return ResponseEntity.notFound().build();
        }
    }


    @GetMapping("/{org}/{domain}/{schema}/{version}/{queryId}/instances")
    public ResponseEntity<QueryResult> executeStoredQuery(@PathVariable(ORG) String org, @PathVariable(DOMAIN) String domain, @PathVariable(SCHEMA) String schema, @PathVariable(VERSION) String version, @PathVariable(QUERY_ID) String queryId, @RequestParam(value = SIZE, required = false) Integer size, @RequestParam(value = START, required = false) Integer start, @RequestParam(value = RESTRICT_TO_ORGANIZATIONS, required = false) String restrictToOrganizations, @RequestParam(value = DATABASE_SCOPE, required = false) DatabaseScope databaseScope, @RequestParam(value = SEARCH, required = false) String searchTerm, @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) throws Exception {
        try {
            NexusSchemaReference schemaReference = new NexusSchemaReference(org, domain, schema, version);
            StoredQueryReference storedQueryReference = new StoredQueryReference(schemaReference, queryId);
            QueryParameters parameters = new QueryParameters(databaseScope, null);
            parameters.pagination().setSize(size).setStart(start);
            if (restrictToOrganizations != null) {
                parameters.filter().restrictToOrganizations(restrictToOrganizations.split(","));
            }
            parameters.filter().setQueryString(searchTerm);
            parameters.authorization().setToken(authorization);
            return ResponseEntity.ok(query.queryPropertyGraphByStoredSpecification(storedQueryReference, parameters, null, new OidcAccessToken().setToken(authorization)));
        } catch (RootCollectionNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).build();
        }
    }


    @GetMapping("/{org}/{domain}/{schema}/{version}/{queryId}/instances/reflect/{instanceId}")
    public ResponseEntity<Map> executeStoredReflectionQuery(@PathVariable(ORG) String org, @PathVariable(DOMAIN) String domain, @PathVariable(SCHEMA) String schema, @PathVariable(VERSION) String version, @PathVariable(QUERY_ID) String queryId, @PathVariable(INSTANCE_ID) String instanceId,  @RequestParam(value = RESTRICT_TO_ORGANIZATIONS, required = false) String restrictToOrganizations, @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) throws Exception {
        try {
            NexusInstanceReference nexusInstanceReference = new NexusInstanceReference(org, domain, schema, version, instanceId);
            StoredQueryReference storedQueryReference = new StoredQueryReference(nexusInstanceReference.getNexusSchema(), queryId);
            QueryParameters parameters = new QueryParameters(DatabaseScope.INFERRED, null);
            if (restrictToOrganizations != null) {
                parameters.filter().restrictToOrganizations(restrictToOrganizations.split(","));
            }
            parameters.authorization().setToken(authorization);
            return ResponseEntity.ok(query.reflectQueryPropertyGraphByStoredSpecification(storedQueryReference, parameters, ArangoDocumentReference.fromNexusInstance(nexusInstanceReference), new OidcAccessToken().setToken(authorization)));
        } catch (RootCollectionNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).build();
        }
    }

    @GetMapping("/{org}/{domain}/{schema}/{version}/{queryId}/instances/{instanceId}")
    public ResponseEntity<Map> executeStoredQueryForInstance(@PathVariable(ORG) String org, @PathVariable(DOMAIN) String domain, @PathVariable(SCHEMA) String schema, @PathVariable(VERSION) String version, @PathVariable(QUERY_ID) String queryId, @PathVariable(INSTANCE_ID) String instanceId, @RequestParam(value = RESTRICT_TO_ORGANIZATIONS, required = false) String restrictToOrganizations, @RequestParam(value = DATABASE_SCOPE, required = false) DatabaseScope databaseScope, @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) throws Exception {
        try {

            NexusInstanceReference nexusInstanceReference = new NexusInstanceReference(org, domain, schema, version, instanceId);
            StoredQueryReference storedQueryReference = new StoredQueryReference(nexusInstanceReference.getNexusSchema(), queryId);
            QueryParameters parameters = new QueryParameters(databaseScope, null);
            if (restrictToOrganizations != null) {
                parameters.filter().restrictToOrganizations(restrictToOrganizations.split(","));
            }
            parameters.authorization().setToken(authorization);
            QueryResult<List<Map>> result = query.queryPropertyGraphByStoredSpecification(storedQueryReference, parameters, ArangoDocumentReference.fromNexusInstance(nexusInstanceReference), new OidcAccessToken().setToken(authorization));
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

    @GetMapping("/{org}/{domain}/{schema}/{version}/{queryId}/meta")
    public ResponseEntity<QueryResult> executeMetaQuery(@PathVariable(ORG) String org, @PathVariable(DOMAIN) String domain, @PathVariable(SCHEMA) String schema, @PathVariable(VERSION) String version, @PathVariable(QUERY_ID) String queryId, @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationToken) throws Exception {
        try {
            NexusSchemaReference nexusSchemaReference = new NexusSchemaReference(org, domain, schema, version);
            StoredQueryReference storedQueryReference = new StoredQueryReference(nexusSchemaReference, queryId);
            QueryParameters parameters = new QueryParameters(null, null);
            parameters.authorization().setToken(authorizationToken);
            return ResponseEntity.ok(query.metaQueryPropertyGraphByStoredSpecification(storedQueryReference, parameters));
        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).build();
        }
    }

    @GetMapping(value = "/{org}/{domain}/{schema}/{version}/{queryId}/templates/{templateId}/meta")
    public ResponseEntity<QueryResult> applyFreemarkerTemplateToMetaApi(@PathVariable(ORG) String org, @PathVariable(DOMAIN) String domain, @PathVariable(SCHEMA) String schema, @PathVariable(VERSION) String version, @PathVariable(QUERY_ID) String queryId, @PathVariable(TEMPLATE_ID) String templateId, @ApiParam("Defines if the underlying json (the one the template is applied to) shall be part of the result as well.") @RequestParam(value = "includeOriginalJson", required = false) boolean includeOriginalJson, @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationToken) throws Exception {
        return applyFreemarkerTemplateToMetaApi(org, domain, schema, version, queryId, templateId, "meta", includeOriginalJson, authorizationToken);
    }

    @GetMapping(value = "/{org}/{domain}/{schema}/{version}/{queryId}/templates/{templateId}/libraries/{library}/meta")
    public ResponseEntity<QueryResult> applyFreemarkerTemplateToMetaApi(@PathVariable(ORG) String org, @PathVariable(DOMAIN) String domain, @PathVariable(SCHEMA) String schema, @PathVariable(VERSION) String version, @PathVariable(QUERY_ID) String queryId, @PathVariable(TEMPLATE_ID) String templateId, @PathVariable(LIBRARY) String library, @ApiParam("Defines if the underlying json (the one the template is applied to) shall be part of the result as well.") @RequestParam(value = "includeOriginalJson", required = false) boolean includeOriginalJson, @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationToken) throws Exception {
        try {
            NexusSchemaReference nexusSchemaReference = new NexusSchemaReference(org, domain, schema, version);
            StoredQueryReference storedQueryReference = new StoredQueryReference(nexusSchemaReference, queryId);
            QueryParameters parameters = new QueryParameters(null, null);
            parameters.context().setReturnOriginalJson(includeOriginalJson);
            parameters.context().setLibrary(new StoredLibraryReference(library, templateId));
            parameters.authorization().setToken(authorizationToken);
            Template template = templating.getTemplateById(new StoredTemplateReference(storedQueryReference, templateId));
            QueryResult result = query.metaQueryPropertyGraphByStoredSpecificationAndFreemarkerTemplate(storedQueryReference, template, parameters);
            return ResponseEntity.ok(RestUtils.toJsonResultIfPossible(result));
        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).build();
        }
    }


    @PostMapping(value = "/{org}/{domain}/{schema}/{version}/{queryId}/instances/{instanceId}/templates")
    public ResponseEntity<Map> applyFreemarkerTemplateToApiWithId(@RequestBody String template, @PathVariable(QUERY_ID) String queryId, @PathVariable(ORG) String org, @PathVariable(DOMAIN) String domain, @PathVariable(SCHEMA) String schema, @PathVariable(VERSION) String version, @PathVariable(INSTANCE_ID) String instanceId, @RequestParam(value = RESTRICT_TO_ORGANIZATIONS, required = false) String restrictToOrganizations, @RequestParam(value = DATABASE_SCOPE, required = false) DatabaseScope databaseScope, @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization, @ApiIgnore @RequestParam Map<String, String> allRequestParams) throws Exception {
        try {
            NexusInstanceReference nexusInstanceReference = new NexusInstanceReference(org, domain, schema, version, instanceId);
            StoredQueryReference storedQueryReference = new StoredQueryReference(nexusInstanceReference.getNexusSchema(), queryId);
            QueryParameters parameters = new QueryParameters(databaseScope, null);
            if (restrictToOrganizations != null) {
                parameters.filter().restrictToOrganizations(restrictToOrganizations.split(","));
            }
            parameters.authorization().setToken(authorization);
            Map result = query.queryPropertyGraphByStoredSpecificationAndFreemarkerTemplateWithId(storedQueryReference, template, parameters, nexusInstanceReference, new OidcAccessToken().setToken(authorization));
            return ResponseEntity.ok(result);
        } catch (RootCollectionNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).build();
        }
    }


    @GetMapping(value = "/{org}/{domain}/{schema}/{version}/{queryId}/templates/{templateId}/instances")
    public ResponseEntity<QueryResult> executeStoredQueryWithTemplate(@PathVariable(ORG) String org, @PathVariable(DOMAIN) String domain, @PathVariable(SCHEMA) String schema, @PathVariable(VERSION) String version, @PathVariable(QUERY_ID) String queryId, @PathVariable(TEMPLATE_ID) String templateId, @RequestParam(value = SIZE, required = false) Integer size, @RequestParam(value = START, required = false) Integer start, @RequestParam(value = SEARCH, required = false) String searchTerm, @RequestParam(value = DATABASE_SCOPE, required = false) DatabaseScope databaseScope, @RequestParam(value = RESTRICT_TO_ORGANIZATIONS, required = false) String restrictToOrganizations, @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization, @ApiParam("Defines if the underlying json (the one the template is applied to) shall be part of the result as well.") @RequestParam(value = "includeOriginalJson", required = false) boolean includeOriginalJson, @ApiIgnore @RequestParam Map<String, String> allRequestParams) throws Exception {
        return executeStoredQueryWithTemplate(org, domain, schema, version, queryId, templateId, "instances", size, start, searchTerm, databaseScope, restrictToOrganizations, authorization, includeOriginalJson, allRequestParams);
    }


    @GetMapping(value = "/{org}/{domain}/{schema}/{version}/{queryId}/templates/{templateId}/libraries/{library}/instances")
    public ResponseEntity<QueryResult> executeStoredQueryWithTemplate(@PathVariable(ORG) String org, @PathVariable(DOMAIN) String domain, @PathVariable(SCHEMA) String schema, @PathVariable(VERSION) String version, @PathVariable(QUERY_ID) String queryId, @PathVariable(TEMPLATE_ID) String templateId, @PathVariable(LIBRARY) String library, @RequestParam(value = SIZE, required = false) Integer size, @RequestParam(value = START, required = false) Integer start, @RequestParam(value = SEARCH, required = false) String searchTerm, @RequestParam(value = DATABASE_SCOPE, required = false) DatabaseScope databaseScope, @RequestParam(value = RESTRICT_TO_ORGANIZATIONS, required = false) String restrictToOrganizations, @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization, @ApiParam("Defines if the underlying json (the one the template is applied to) shall be part of the result as well.") @RequestParam(value = "includeOriginalJson", required = false) boolean includeOriginalJson, @ApiIgnore @RequestParam Map<String, String> allRequestParams) throws Exception {
        NexusSchemaReference schemaReference = new NexusSchemaReference(org, domain, schema, version);
        StoredTemplateReference templateReference = new StoredTemplateReference(new StoredQueryReference(schemaReference, queryId), templateId);
        Template template = templating.getTemplateById(templateReference);
        QueryParameters parameters = new QueryParameters(databaseScope, null);
        parameters.pagination().setSize(size).setStart(start);
        parameters.context().setReturnOriginalJson(includeOriginalJson);
        parameters.context().setLibrary(new StoredLibraryReference(library, templateId));
        if (restrictToOrganizations != null) {
            parameters.filter().restrictToOrganizations(restrictToOrganizations.split(","));
        }
        parameters.filter().setQueryString(searchTerm);
        parameters.authorization().setToken(authorization);
        return ResponseEntity.ok(query.queryPropertyGraphByStoredSpecificationAndFreemarkerTemplate(templateReference.getQueryReference(), template.getTemplateContent(), parameters, new OidcAccessToken().setToken(authorization)));
    }

    @GetMapping(value = "/{org}/{domain}/{schema}/{version}/{queryId}/templates/{templateId}/instances/{instanceId}")
    public ResponseEntity<Map> executeStoredQueryWithTemplate(@PathVariable(ORG) String org, @PathVariable(DOMAIN) String domain, @PathVariable(SCHEMA) String schema, @PathVariable(VERSION) String version, @PathVariable(QUERY_ID) String queryId, @PathVariable(TEMPLATE_ID) String templateId, @PathVariable(INSTANCE_ID) String instanceId, @RequestParam(value = DATABASE_SCOPE, required = false) DatabaseScope databaseScope, @RequestParam(value = RESTRICT_TO_ORGANIZATIONS, required = false) String restrictToOrganizations, @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization, @ApiIgnore @RequestParam Map<String, String> allRequestParams) throws Exception {
        NexusInstanceReference nexusInstanceReference = new NexusInstanceReference(org, domain, schema, version, instanceId);
        StoredTemplateReference templateReference = new StoredTemplateReference(new StoredQueryReference(nexusInstanceReference.getNexusSchema(), queryId), templateId);
        Template template = templating.getTemplateById(templateReference);
        StoredQueryReference storedQueryReference = new StoredQueryReference(nexusInstanceReference.getNexusSchema(), queryId);
        QueryParameters parameters = new QueryParameters(databaseScope, null);
        if (restrictToOrganizations != null) {
            parameters.filter().restrictToOrganizations(restrictToOrganizations.split(","));
        }
        parameters.context().setLibrary(new StoredLibraryReference("instances", templateId));
        parameters.authorization().setToken(authorization);
        Map result = query.queryPropertyGraphByStoredSpecificationAndFreemarkerTemplateWithId(storedQueryReference, template.getTemplateContent(), parameters, nexusInstanceReference, new OidcAccessToken().setToken(authorization));
        return ResponseEntity.ok(result);
    }


}
