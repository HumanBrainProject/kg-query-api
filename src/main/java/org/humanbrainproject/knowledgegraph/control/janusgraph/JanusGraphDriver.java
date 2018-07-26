package org.humanbrainproject.knowledgegraph.control.janusgraph;


import org.apache.tinkerpop.gremlin.driver.Cluster;
import org.apache.tinkerpop.gremlin.driver.MessageSerializer;
import org.apache.tinkerpop.gremlin.driver.remote.DriverRemoteConnection;
import org.apache.tinkerpop.gremlin.driver.ser.GryoMessageSerializerV1d0;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.io.gryo.GryoMapper;
import org.apache.tinkerpop.gremlin.structure.util.empty.EmptyGraph;
import org.janusgraph.graphdb.tinkerpop.JanusGraphIoRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * The connection establishment to the JanusGraphCluster including message serialization configuration.
 */
@Component
@Scope(scopeName = "singleton")
public class JanusGraphDriver {

    @Value("${org.humanbrainproject.knowledgegraph.janusgraph.contactPoint}")
    String contactPoint;

    MessageSerializer serializer;
    Cluster cluster;
    int closeCounter = 0;


    private MessageSerializer getMessageSerializer(){
        if(serializer==null) {
            serializer = new GryoMessageSerializerV1d0(GryoMapper.build().addRegistry(JanusGraphIoRegistry.getInstance()));
        }
        return serializer;
    }

    private synchronized Cluster getCluster() {
        if(closeCounter++>1000){
            cluster.close();
            cluster=null;
            closeCounter=0;
        }
        if(cluster==null || cluster.isClosed()){
            cluster = Cluster.build().addContactPoint(contactPoint).serializer(getMessageSerializer()).create();
        }
        return cluster;
    }

    public GraphTraversalSource getGraphTraversalSource(){
        return EmptyGraph.instance().traversal().withRemote(DriverRemoteConnection.using(getCluster()));
    }

    public GraphTraversalSource getOlapGraphTraversalSource(){
        Cluster cluster = Cluster.build().addContactPoint(contactPoint).serializer(getMessageSerializer()).create();
        return EmptyGraph.instance().traversal().withComputer().withRemote(DriverRemoteConnection.using(cluster));
    }

    public void closeCluster(){
        if(cluster!=null){
            cluster.close();
        }
    }
}
