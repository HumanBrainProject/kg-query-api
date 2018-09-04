package org.humanbrainproject.knowledgegraph.api.query;

import io.swagger.annotations.Api;
import org.humanbrainproject.knowledgegraph.boundary.query.ArangoQuery;
import org.humanbrainproject.knowledgegraph.boundary.query.Templating;
import org.humanbrainproject.knowledgegraph.entity.query.SpatialSearchResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.ws.rs.core.MediaType;
import java.util.Set;

@RestController
@RequestMapping(value = "/spatial", produces = MediaType.APPLICATION_JSON)
@Api(value="/spatial", description = "The API for querying the knowledge graph")
public class SpatialQueryAPI {

    @Autowired
    ArangoQuery query;

    @Autowired
    Templating templating;


    @GetMapping("/")
    public Set<SpatialSearchResult> getElementsByBox(@RequestParam(value="box", required=false) String box, @RequestParam(value="referencespace") String referenceSpace, @RequestParam(value="returnKeys", defaultValue = "true") boolean returnKeys, @RequestParam(value="returnCoordinates", defaultValue = "true") boolean returnCoordinates, @RequestParam(value="returnLabels", defaultValue = "true") boolean returnLabels) {
        return null;
    }


    @GetMapping("/volumes/{space}/{label}")
    public Set<SpatialSearchResult> getElementsByLabel(@PathVariable(value="space") String space, @PathVariable(value="label") String labelSpace, @RequestParam(value="returnKeys", defaultValue = "true") boolean returnKeys, @RequestParam(value="returnCoordinates", defaultValue = "true") boolean returnCoordinates, @RequestParam(value="returnLabels", defaultValue = "true") boolean returnLabels) {
        return null;
    }

}
