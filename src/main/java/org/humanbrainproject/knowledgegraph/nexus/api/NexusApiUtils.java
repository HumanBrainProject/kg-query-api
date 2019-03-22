package org.humanbrainproject.knowledgegraph.nexus.api;


import io.swagger.annotations.Api;
import org.humanbrainproject.knowledgegraph.annotations.ToBeTested;
import org.humanbrainproject.knowledgegraph.commons.api.RestUtils;
import org.humanbrainproject.knowledgegraph.commons.authorization.control.AuthorizationContext;
import org.humanbrainproject.knowledgegraph.nexus.entity.FileStructureData;
import org.humanbrainproject.knowledgegraph.nexus.boundary.NexusUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.Map;
import java.util.zip.ZipInputStream;

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
    public ResponseEntity<Map> uploadFilestructure(InputStream payload,  @RequestParam(value = NO_DELETION, required = false) boolean noDeletion, @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationToken) {
        authorizationContext.setCredential(authorizationToken);
        BufferedInputStream bis = new BufferedInputStream(payload);
        FileStructureData fs = new FileStructureData(new ZipInputStream(bis), noDeletion);
        nexusUtils.uploadFileStructure(fs);
        return ResponseEntity.ok().build();

    }
}
