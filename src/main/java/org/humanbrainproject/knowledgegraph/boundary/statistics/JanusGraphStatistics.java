package org.humanbrainproject.knowledgegraph.boundary.statistics;

import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.humanbrainproject.knowledgegraph.control.janusgraph.JanusGraphDriver;
import org.janusgraph.graphdb.olap.computer.FulgoraGraphComputer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class JanusGraphStatistics {

    @Autowired
    JanusGraphDriver janusGraphCluster;

    public void stats(){
        GraphTraversalSource olapGraphTraversalSource = janusGraphCluster.getGraphTraversalSource();
        System.out.println(olapGraphTraversalSource.withComputer(FulgoraGraphComputer.class).V().count().next());
    }
}
