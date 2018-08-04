package org.humanbrainproject.knowledgegraph.control;

import org.humanbrainproject.knowledgegraph.api.indexation.ArangoIndexationAPI;
import org.humanbrainproject.knowledgegraph.entity.jsonld.JsonLdEdge;
import org.humanbrainproject.knowledgegraph.entity.jsonld.JsonLdVertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.configurationprocessor.json.JSONException;

import java.util.List;


public abstract class VertexRepository<T> {

    public static final String UNRESOLVED_LINKS = "http://schema.hbp.eu/propertygraph/unresolved";

    @Autowired
    Configuration configuration;

    protected Logger logger = LoggerFactory.getLogger(VertexRepository.class);


    public void uploadToPropertyGraph(List<JsonLdVertex> vertices, T transactionOrConnection) throws JSONException {
        for (JsonLdVertex vertex : vertices) {
            Long revision = getRevisionById(vertex, transactionOrConnection);
            if (revision != null) {
                if (revision < vertex.getRevision()) {
                    updateVertex(vertex, transactionOrConnection);
                } else {
                   logger.debug("No new revision - no update");
                }
            } else {
                insertVertex(vertex, transactionOrConnection);
            }
        }
        for (JsonLdVertex vertex : vertices) {
            //We update the edge in a second step to make sure, instances created within the same step are reflected when resolving.
            for (int i = 0; i < vertex.getEdges().size(); i++) {
                JsonLdEdge edge = vertex.getEdges().get(i);
                if(edge.isEmbedded() || isInternalEdge(edge, configuration.getNexusBase())) {
                    if(!hasEdge(vertex, edge, transactionOrConnection)) {
                        createEdge(vertex, edge, i, transactionOrConnection);
                    }
                    else{
                        updateEdge(vertex, edge, i, transactionOrConnection);
                    }
                }
            }
        }

        for (JsonLdVertex vertex : vertices) {
            for (int i = 0; i < vertex.getEdges().size(); i++) {
                updateUnresolved(vertex, transactionOrConnection);
            }
        }
    };


    protected boolean isInternalEdge(JsonLdEdge edge, String nexusBase){
        if(edge.getReference()==null || edge.getReference().trim().isEmpty() || !edge.getReference().startsWith(nexusBase)){
            return false;
        }
        return true;
    }

    protected abstract Long getRevisionById(JsonLdVertex vertex, T transactionOrConnection);

    public abstract void deleteVertex(String entityName, String identifier, T transactionOrConnection);

    protected abstract void updateVertex(JsonLdVertex vertex, T transactionOrConnection) throws JSONException;

    protected abstract void insertVertex(JsonLdVertex vertex, T transactionOrConnection) throws JSONException;

    protected abstract boolean hasEdge(JsonLdVertex vertex, JsonLdEdge edge, T transactionOrConnection) throws JSONException;

    protected abstract void createEdge(JsonLdVertex vertex, JsonLdEdge edge, int orderNumber, T transactionOrConnection) throws JSONException;

    protected abstract void updateEdge(JsonLdVertex vertex, JsonLdEdge edge, int orderNumber, T transactionOrConnection) throws JSONException;

    public abstract void updateUnresolved(JsonLdVertex vertex, T transactionOrConnection);

}
