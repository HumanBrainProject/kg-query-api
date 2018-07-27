package org.humanbrainproject.knowledgegraph.boundary.indexation;

import org.humanbrainproject.knowledgegraph.control.jsonld.JsonLdStandardization;
import org.humanbrainproject.knowledgegraph.control.jsonld.JsonLdToVerticesAndEdges;
import org.humanbrainproject.knowledgegraph.control.neo4j.Neo4jRepository;
import org.humanbrainproject.knowledgegraph.control.neo4j.Neo4JDriver;
import org.humanbrainproject.knowledgegraph.entity.jsonld.JsonLdVertex;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.TransactionWork;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.configurationprocessor.json.JSONException;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CypherIndexation {

    @Autowired
    JsonLdStandardization jsonLdStandardization;

    @Autowired
    JsonLdToVerticesAndEdges jsonLdToVerticesAndEdges;

    @Autowired
    Neo4jRepository cypherUploader;

    @Autowired
    Neo4JDriver neo4J;


    void transactionalJsonLdUpload(List<JsonLdVertex> vertices) {
        try(Session session = neo4J.getDriver().session()) {
            session.writeTransaction((TransactionWork<Void>) transaction -> {
                try {
                    cypherUploader.uploadToPropertyGraph(vertices, transaction);
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
                return null;
            });
        }
    }

    public void clearGraph(){
        try(Session session = neo4J.getDriver().session()) {
            session.writeTransaction((TransactionWork<Void>) transaction -> {
                        transaction.run("MATCH (n)\n" +
                                "OPTIONAL MATCH (n)-[r]-()\n" +
                                "DELETE n, r");
                        return null; }
            );
        }
    }

}
