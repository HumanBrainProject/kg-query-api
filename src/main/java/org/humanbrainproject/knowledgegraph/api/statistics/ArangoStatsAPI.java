package org.humanbrainproject.knowledgegraph.api.statistics;

import org.humanbrainproject.knowledgegraph.boundary.statistics.ArangoStatistics;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.ws.rs.core.MediaType;
import java.util.Map;

@RestController
@RequestMapping(value = "/kg/statistics", produces = MediaType.APPLICATION_JSON)
public class ArangoStatsAPI  {

    @Autowired
    ArangoStatistics statistics;

    @GetMapping
    public Map<String, Object> getStatistics(){
        return statistics.getStructure();
    }
}
