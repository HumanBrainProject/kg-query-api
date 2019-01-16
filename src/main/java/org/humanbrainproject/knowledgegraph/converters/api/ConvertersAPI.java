package org.humanbrainproject.knowledgegraph.converters.api;

import io.swagger.annotations.Api;
import org.humanbrainproject.knowledgegraph.annotations.ToBeTested;
import org.humanbrainproject.knowledgegraph.commons.InternalApi;
import org.humanbrainproject.knowledgegraph.commons.api.RestUtils;
import org.humanbrainproject.knowledgegraph.commons.jsonld.control.JsonTransformer;
import org.humanbrainproject.knowledgegraph.converters.boundary.Converter;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusSchemaReference;
import org.humanbrainproject.knowledgegraph.query.entity.JsonDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.ws.rs.core.MediaType;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import static org.humanbrainproject.knowledgegraph.commons.api.ParameterConstants.*;
@RestController
@RequestMapping(value = "/coverters")
@InternalApi
@Api(value="/converters", description = "The converter mechanisms")
@ToBeTested(easy = true)
public class ConvertersAPI {

    @Autowired
    Converter converter;

    @Autowired
    JsonTransformer jsonTransformer;

    private Logger logger = LoggerFactory.getLogger(ConvertersAPI.class);

    private String getTimestamp(String timestamp){
        return timestamp!=null ? timestamp : ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT);
    }

    @PostMapping(value="/shacl-to-editor/{"+ORG+"}/{"+DOMAIN+"}/{"+SCHEMA+"}/{"+VERSION+"}", consumes = {MediaType.APPLICATION_JSON, RestUtils.APPLICATION_LD_JSON}, produces = MediaType.APPLICATION_JSON)
    public ResponseEntity<Map> shacl2editor(@RequestBody String payload, @PathVariable(ORG) String organization, @PathVariable(DOMAIN) String domain, @PathVariable(SCHEMA) String schema, @PathVariable(VERSION) String schemaVersion) {
        JsonDocument jsonDocument = converter.convertShaclToEditor(new NexusSchemaReference(organization, domain, schema, schemaVersion));
        if(jsonDocument==null){
            return ResponseEntity.notFound().build();
        }
        else{
            return ResponseEntity.ok(jsonDocument);
        }
    }
}
