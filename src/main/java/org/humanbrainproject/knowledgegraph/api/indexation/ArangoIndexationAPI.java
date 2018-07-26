package org.humanbrainproject.knowledgegraph.api.indexation;

import org.humanbrainproject.knowledgegraph.boundary.indexation.ArangoIndexation;
import org.humanbrainproject.knowledgegraph.control.VertexRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.configurationprocessor.json.JSONException;
import org.springframework.web.bind.annotation.*;

import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.util.logging.Logger;

@RestController
@RequestMapping(value = "/arango", consumes = {MediaType.APPLICATION_JSON, "application/ld+json"}, produces = MediaType.APPLICATION_JSON)
public class ArangoIndexationAPI implements KGIndexationAPI {

    @Autowired
    ArangoIndexation indexer;

    protected Logger log = Logger.getLogger(ArangoIndexationAPI.class.getName());

    @PostMapping
    public void uploadToPropertyGraph(@RequestBody String payload, @QueryParam("defaultNamespace") String defaultNamespace, @QueryParam("vertexLabel") String vertexLabel) throws IOException, JSONException {
        log.info(String.format("Processing %s", payload));
        indexer.uploadJsonOrJsonLd(payload, defaultNamespace, vertexLabel);
    }

    @DeleteMapping
    public void clearGraph(){
        indexer.clearGraph();
    }

}
