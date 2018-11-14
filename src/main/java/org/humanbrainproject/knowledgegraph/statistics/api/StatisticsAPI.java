package org.humanbrainproject.knowledgegraph.statistics.api;

import org.humanbrainproject.knowledgegraph.statistics.boundary.Statistics;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import javax.ws.rs.core.MediaType;
import java.util.Map;

@ApiIgnore
@RestController
@RequestMapping(value = "/statistics", produces = MediaType.APPLICATION_JSON)
public class StatisticsAPI {

    @Autowired
    Statistics statistics;

    @GetMapping
    public Map<String, Object> getStatistics(){
        return statistics.getStructure();
    }



}
