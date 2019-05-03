package org.humanbrainproject.knowledgegraph.nexus.api;


import io.swagger.annotations.Api;
import org.apache.solr.client.solrj.SolrServerException;
import org.humanbrainproject.knowledgegraph.annotations.ToBeTested;
import org.humanbrainproject.knowledgegraph.commons.authorization.control.AuthorizationContext;
import org.humanbrainproject.knowledgegraph.nexus.boundary.NexusUtils;
import org.humanbrainproject.knowledgegraph.nexus.entity.UploadStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.configurationprocessor.json.JSONException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

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
    final String IS_SIMULATION= "simulate";

    @PostMapping(consumes = {MediaType.MULTIPART_FORM_DATA})
    public ResponseEntity<Map> uploadFilestructure(@RequestParam("file") MultipartFile payload, @RequestParam(value = NO_DELETION, required = false) boolean noDeletion, @RequestParam(value = IS_SIMULATION, required = false) boolean isSimulation, @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationToken)
            throws IOException, SolrServerException, JSONException, NoSuchAlgorithmException {
        if(!payload.isEmpty()){
            authorizationContext.populateAuthorizationContext(authorizationToken);
            UUID uuid = nexusUtils.uploadFileStructure(payload.getInputStream(), noDeletion, isSimulation);
            Map<String, String> result = new HashMap<>();
            result.put("url", ServletUriComponentsBuilder.fromCurrentRequestUri().toUriString() + "/" + uuid.toString());
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.badRequest().build();
        }

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
        } catch (NotFoundException e){
            return ResponseEntity.notFound().build();
        }catch (ExecutionException e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }catch (InterruptedException e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }

    }
}
