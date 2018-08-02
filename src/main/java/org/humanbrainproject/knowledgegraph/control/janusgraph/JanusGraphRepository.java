package org.humanbrainproject.knowledgegraph.control.janusgraph;


import com.github.jsonldjava.core.JsonLdConsts;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.humanbrainproject.knowledgegraph.api.indexation.ArangoIndexationAPI;
import org.humanbrainproject.knowledgegraph.control.Configuration;
import org.humanbrainproject.knowledgegraph.entity.jsonld.JsonLdEdge;
import org.humanbrainproject.knowledgegraph.entity.jsonld.JsonLdProperty;
import org.humanbrainproject.knowledgegraph.entity.jsonld.JsonLdVertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import sun.security.krb5.Config;

import java.util.List;
import java.util.Optional;

/**
 * Provides the functionality to upload previously built vertices and edges structures ({@see JsonLdToVerticesAndEdges})
 * to a property graph using the Gremlin language.
 */
@Component
public class JanusGraphRepository {

    public static final String UNRESOLVED_LINKS = "http://schema.hbp.eu/propertygraph/unresolved";

    Logger logger = LoggerFactory.getLogger(JanusGraphRepository.class);

    @Autowired
    Configuration configuration;

    public void uploadToPropertyGraph(GraphTraversalSource g, List<JsonLdVertex> vertices) {
        for (JsonLdVertex vertex : vertices) {
            deleteUpdateOrCreateVerticesInPropertyGraph(vertex, g);
        }
        for (JsonLdVertex vertex : vertices) {
            if (vertex.getDeprecated() == null || !vertex.getDeprecated()) {
                createEdgesInPropertyGraph(vertex, g);
            }
        }
    }

    /**
     * Create/update the outgoing edges of the given vertex in the graph.
     *
     * @param newVertex
     * @param g
     */

    private void createEdgesInPropertyGraph(JsonLdVertex newVertex, GraphTraversalSource g) {
        Vertex origin = g.V().has(JsonLdConsts.ID, newVertex.getId()).tryNext().orElse(null);
        if (origin != null) {
            g.V(origin).outE().drop().tryNext();
            for (JsonLdEdge edge : newVertex.getEdges()) {
                Optional<Edge> e = g.V(origin).as("o").V().has(JsonLdConsts.ID, edge.getReference()).as("t").addE(edge.getName()).from("o").to("t").tryNext();
                if (!e.isPresent()) {
                    //We cannot resolve the relation inside the graph.
                    handleUnresolvedRelation(g, origin, edge);
                }
            }
        }
    }

    private void handleUnresolvedRelation(GraphTraversalSource g, Vertex origin, JsonLdEdge edge) {
        List<Vertex> unresolvedVertices = g.V(origin).out(edge.getName()).hasLabel(UNRESOLVED_LINKS).has(JsonLdConsts.ID, edge.getReference()).toList();
        if(unresolvedVertices.isEmpty()){
            //The relation is not yet registered as unresolved in the graph. Let's do it!
            g.addV(UNRESOLVED_LINKS).property(JsonLdConsts.ID, edge.getReference()).tryNext();
        }
    }

    /**
     * Deletes (if the payload is tagged as deprecated), updates (if the vertex already exists and has a lower revision
     * number) or inserts (if the id is unknown) the vertex to the graph
     *
     * @param newVertex
     * @param g
     */
    private void deleteUpdateOrCreateVerticesInPropertyGraph(JsonLdVertex newVertex, GraphTraversalSource g) {
        List<Vertex> vertices = g.V().has(JsonLdConsts.ID, newVertex.getId()).toList();
        if (!vertices.isEmpty() && newVertex.getDeprecated() != null && newVertex.getDeprecated()) {
            //We remove deprecated vertices
            removeVertices(g, vertices);
        } else {
            if (!vertices.isEmpty()) {
                //Update
                logger.info("Update vertex");
                updateVertexPropertiesIfNewRevision(newVertex, g, vertices.get(0));
            }
            if (vertices.isEmpty()) {
                //Insert
                logger.info("Insert vertex");
                updateProperties(newVertex, g.addV(newVertex.getType())).tryNext();
                logger.debug("Add new vertex {}", newVertex);
            }
            if(vertices.size()>1){
                //Remove duplicates
                removeVertices(g, vertices.subList(1, vertices.size()));
            }
        }
    }

    /**
     * Helper function to update the properties of a vertex if the revision of the new vertex is bigger than the existing one in the graph
     *
     * @param newVertex
     * @param g
     * @param fromGraph
     */
    private void updateVertexPropertiesIfNewRevision(JsonLdVertex newVertex, GraphTraversalSource g, Vertex fromGraph) {
        Object rev = g.V(fromGraph).has(configuration.getRev()).values(configuration.getRev()).tryNext().orElse(null);
        if (rev == null || newVertex.getRevision() == null || !(rev instanceof Number) || newVertex.getRevision() > ((Number)rev).intValue()) {
            g.V(fromGraph).properties().drop().tryNext();
            logger.debug("Removed  all properties of vertex {}", fromGraph);
            updateProperties(newVertex, g.V(fromGraph)).tryNext();

        }
    }

    /**
     * Removes all passed vertices from the graph
     *
     * @param g
     * @param vertices
     */
    private void removeVertices(GraphTraversalSource g, List<Vertex> vertices) {
        for (Vertex vertex : vertices) {
            g.V(vertex).drop().tryNext();
            logger.info("Removed vertex {}", vertex);
        }
    }

    /**
     * Takes a new vertex and saves its properties into the current location of the graph traversal.
     * Ensures that @id and revision properties are set even if the new vertex doesn't contain them
     * in its payload (e.g. because it originates from a nested object and therefore inherits these
     * values from its parent)
     *
     * @param newVertex
     * @param traversal
     * @return the traversal instance originally passed as an argument (for chaining)
     */
    private GraphTraversal<Vertex, Vertex> updateProperties(JsonLdVertex newVertex, GraphTraversal<Vertex, Vertex> traversal) {
        boolean idDefined = false;
        boolean revisionDefined = false;
        for (JsonLdProperty property : newVertex.getProperties()) {
            if (property.getName().equals(JsonLdConsts.ID)) {
                idDefined = true;
            }
            if (property.getName().equals(configuration.getRev())) {
                revisionDefined = true;
            }
            traversal.property(property.getName(), property.getValue());
            logger.debug("Add property {} for vertex {}", property.getName(), newVertex);
        }
        if (!idDefined) {
            traversal.property(JsonLdConsts.ID, newVertex.getId());
            logger.debug("Add property {} for vertex {}", JsonLdConsts.ID, newVertex);
        }
        if (!revisionDefined) {
            traversal.property(configuration.getRev(), newVertex.getRevision());
            logger.debug("Add property {} for vertex {}", configuration.getRev(), newVertex);
        }
        return traversal;
    }


    public void clearGraph(GraphTraversalSource g){
        g.V().drop().tryNext();
    }

}
