package org.humanbrainproject.knowledgegraph.admin.api;

import io.swagger.annotations.Api;
import org.humanbrainproject.knowledgegraph.admin.boundary.Admin;
import org.humanbrainproject.knowledgegraph.annotations.ToBeTested;
import org.humanbrainproject.knowledgegraph.commons.InternalApi;
import org.humanbrainproject.knowledgegraph.commons.authorization.control.AuthorizationContext;
import org.humanbrainproject.knowledgegraph.commons.jsonld.control.JsonTransformer;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.exceptions.RootCollectionNotFoundException;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.exceptions.StoredQueryNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;

import javax.ws.rs.core.MediaType;
import java.util.List;

@RestController
@RequestMapping(value = "/admin", produces = MediaType.APPLICATION_JSON)
@ToBeTested(easy = true)
@Api(value = "/admin", description = "The API for administrative tasks")
@InternalApi
public class AdminApi {

    @Autowired
    AuthorizationContext authorizationContext;

    @Autowired
    Admin admin;

    @Autowired
    JsonTransformer jsonTransformer;

    @PostMapping(value = "/privateSpaces/{space}")
    public ResponseEntity<Void> createPrivateSpace(@RequestBody String payload, @PathVariable("space") String space, @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationToken) throws Exception {
        try {
            authorizationContext.populateAuthorizationContext(authorizationToken);
            List<String> managerGroups = jsonTransformer.parseToListOfStrings(payload);
            admin.createPrivateSpace(space, managerGroups);

            return ResponseEntity.status(HttpStatus.CREATED).build();
        } catch (StoredQueryNotFoundException e){
            return ResponseEntity.notFound().build();
        } catch (RootCollectionNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).build();
        }
    }


    @PostMapping(value = "/privateSpaces/{space}/clients/{client}/id/{clientId}")
    public ResponseEntity<Void> addServiceClientToSpace(@RequestBody String payload, @PathVariable("space") String space, @PathVariable("client") String client, @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationToken) throws Exception {
        try {
            authorizationContext.populateAuthorizationContext(authorizationToken);
            List<String> resolvers = jsonTransformer.parseToListOfStrings(payload);
            admin.addServiceClientToPrivateSpace(space, client, resolvers);
            return ResponseEntity.status(HttpStatus.CREATED).build();
        } catch (StoredQueryNotFoundException e){
            return ResponseEntity.notFound().build();
        } catch (RootCollectionNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).build();
        }
    }
}
