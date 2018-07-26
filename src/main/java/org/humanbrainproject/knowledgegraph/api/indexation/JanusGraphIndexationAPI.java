package org.humanbrainproject.knowledgegraph.api.indexation;

import org.humanbrainproject.knowledgegraph.boundary.indexation.JanusGraphIndexation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.configurationprocessor.json.JSONException;
import org.springframework.web.bind.annotation.*;

import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import java.io.IOException;

@RestController
@RequestMapping(value = "/janusgraph", consumes = {MediaType.APPLICATION_JSON, "application/ld+json"}, produces = "application/ld+json")
public class JanusGraphIndexationAPI implements KGIndexationAPI {

    @Autowired
    JanusGraphIndexation jsonLd2Gremlin;


    @PostMapping
    public void uploadToPropertyGraph(@RequestBody String payload, @QueryParam("defaultNamespace") String defaultNamespace, @QueryParam("vertexLabel") String vertexLabel) throws IOException, JSONException {
        jsonLd2Gremlin.uploadJsonOrJsonLd(payload, defaultNamespace, vertexLabel);
    }


    @DeleteMapping
    public void clearGraph(){
        jsonLd2Gremlin.clearGraph();
    }

}
