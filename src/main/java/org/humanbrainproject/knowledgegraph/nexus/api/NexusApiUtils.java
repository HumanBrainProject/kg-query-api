package org.humanbrainproject.knowledgegraph.nexus.api;


import io.swagger.annotations.Api;
import org.apache.solr.client.solrj.SolrServerException;
import org.humanbrainproject.knowledgegraph.annotations.ToBeTested;
import org.humanbrainproject.knowledgegraph.commons.api.RestUtils;
import org.humanbrainproject.knowledgegraph.commons.authorization.control.AuthorizationContext;
import org.humanbrainproject.knowledgegraph.nexus.boundary.NexusUtils;
import org.humanbrainproject.knowledgegraph.nexus.entity.UploadStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.configurationprocessor.json.JSONException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping(value = "/nexus", produces = MediaType.APPLICATION_JSON)
@Api(value = "/nexus", description = "The helper API for nexus")
@ToBeTested(easy = true)
public class NexusApiUtils {

    @Autowired
    NexusUtils nexusUtils;

    @Autowired
    AuthorizationContext authorizationContext;

    final String NO_DELETION = "nodeletion";


    @PostMapping(consumes = {RestUtils.APPLICATION_ZIP})
    public ResponseEntity<Map> uploadFilestructure(InputStream payload,  @RequestParam(value = NO_DELETION, required = false) boolean noDeletion, @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationToken)
            throws IOException, JSONException, SolrServerException {
        authorizationContext.populateAuthorizationContext(authorizationToken);
        UUID uuid = nexusUtils.uploadFileStructure(payload, noDeletion);
        Map<String, String> result = new HashMap<>();

        result.put("url", ServletUriComponentsBuilder.fromCurrentRequestUri().toUriString() + "/" + uuid.toString());
        return ResponseEntity.ok(result);

    }

    @GetMapping(value = "/{uuid}")
    public ResponseEntity<UploadStatus> getUploadStatus(@PathVariable(value = "uuid") String uuid, @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationToken) {
        authorizationContext.populateAuthorizationContext(authorizationToken);
        try{
            UploadStatus s = nexusUtils.retreiveUploadStatus(UUID.fromString(uuid));
            if(s != null){
                return ResponseEntity.ok(s);
            } else{
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }

    }
}
