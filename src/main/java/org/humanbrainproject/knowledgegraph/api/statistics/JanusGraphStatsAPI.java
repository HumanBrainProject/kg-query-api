package org.humanbrainproject.knowledgegraph.api.statistics;

import org.humanbrainproject.knowledgegraph.boundary.statistics.JanusGraphStatistics;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/stats")
public class JanusGraphStatsAPI {

    @Autowired
    JanusGraphStatistics statistics;

    @GetMapping
    public void getStats(){
        statistics.stats();
    }
}
