package org.humanbrainproject.knowledgegraph.query.api;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.humanbrainproject.knowledgegraph.annotations.ToBeTested;
import org.humanbrainproject.knowledgegraph.commons.InternalApi;
import org.humanbrainproject.knowledgegraph.commons.authorization.control.AuthorizationContext;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.exceptions.RootCollectionNotFoundException;
import org.humanbrainproject.knowledgegraph.context.QueryContext;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusSchemaReference;
import org.humanbrainproject.knowledgegraph.query.boundary.ArangoQuery;
import org.humanbrainproject.knowledgegraph.query.boundary.Templating;
import org.humanbrainproject.knowledgegraph.query.entity.Query;
import org.humanbrainproject.knowledgegraph.query.entity.QueryResult;
import org.humanbrainproject.knowledgegraph.query.entity.StoredQueryReference;
import org.humanbrainproject.knowledgegraph.query.entity.Template;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.Map;

import static org.humanbrainproject.knowledgegraph.commons.api.ParameterConstants.*;


@RestController
@RequestMapping(value = "/internal/api/query", produces = MediaType.APPLICATION_JSON)
@Api(value = "/internal/api/query", description = "The API for querying the knowledge graph")
@InternalApi
@ToBeTested(easy = true)
public class QueryInternalAPI {


    @Autowired
    AuthorizationContext authorizationContext;

    @Autowired
    ArangoQuery query;

    @Autowired
    Templating templating;


    @PutMapping(value = "/{"+ORG+"}/{"+ DOMAIN+"}/{"+SCHEMA+"}/{"+VERSION+"}/{"+QUERY_ID+"}/templates/{"+TEMPLATE_ID+"}")
    public ResponseEntity<Void> saveFreemarkerTemplate(@RequestBody String template, @PathVariable(ORG) String org, @PathVariable(DOMAIN) String domain, @PathVariable(SCHEMA) String schema, @PathVariable(VERSION) String version, @PathVariable(QUERY_ID) String queryId, @PathVariable(TEMPLATE_ID) String templateId, @RequestParam(value = "lib", required = false) String library, @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        authorizationContext.populateAuthorizationContext(authorization);
        Template t = new Template(new StoredQueryReference(new NexusSchemaReference(org, domain, schema, version), queryId), templateId, template, library == null ? templateId : library);
        templating.saveTemplate(t);
        return null;
    }

    @PutMapping(value = "/templates/{"+TEMPLATE_ID+"}/libraries/{"+LIBRARY+"}")
    public ResponseEntity<Void> saveFreemarkerLibrary(@RequestBody String library, @PathVariable(TEMPLATE_ID) String templateId, @PathVariable(LIBRARY) String libraryId, @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) throws Exception {
        authorizationContext.populateAuthorizationContext(authorization);
        templating.saveLibrary(library, libraryId, templateId);
        return null;
    }

    @ApiOperation(value="Fetch UUIDS")
    @GetMapping(value = "/{"+ORG+"}/{"+ DOMAIN+"}/{"+SCHEMA+"}/{"+VERSION+"}/instancesid")
    public ResponseEntity<QueryResult> queryResolveByIdentifier(@PathVariable(ORG) String org, @PathVariable(DOMAIN) String domain, @PathVariable(SCHEMA) String schema, @PathVariable(VERSION) String version, @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationToken) {
        try {authorizationContext.populateAuthorizationContext(authorizationToken);
            Query query = new Query(identifierQuery, new NexusSchemaReference(org, domain, schema, version), "https://schema.hbp.eu/myQuery/");
            QueryResult<List<Map>> result = this.query.queryPropertyGraphBySpecification(query);
            return ResponseEntity.ok(result);
        } catch (RootCollectionNotFoundException e) {
            return ResponseEntity.ok(QueryResult.createEmptyResult());
        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).build();
        } catch(HttpServerErrorException e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } catch (Exception e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private String identifierQuery = "{" +
            "  \"@context\": {" +
            "    \"@vocab\": \"https://schema.hbp.eu/graphQuery/\"," +
            "    \"query\": \"https://schema.hbp.eu/myQuery/\"," +
            "    \"fieldname\": {" +
            "      \"@id\": \"fieldname\"," +
            "      \"@type\": \"@id\"" +
            "    }," +
            "    \"relative_path\": {" +
            "      \"@id\": \"relative_path\"," +
            "      \"@type\": \"@id\"" +
            "    }" +
            "  }," +
            "  \"fields\": [" +
            "    {" +
            "      \"fieldname\": \"query:identifier\"," +
            "      \"relative_path\": {" +
            "        \"@id\": \"http://schema.org/identifier\"" +
            "      }," +
            "      \"required\":true" +
            "    }," +
            "    {" +
            "      \"fieldname\": \"query:uuid\"," +
            "      \"relative_path\": {" +
            "        \"@id\": \"@id\"" +
            "      }," +
            "      \"required\":true" +
            "    }" +
            "  ]" +
            "}";

}
