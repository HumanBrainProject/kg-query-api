package org.humanbrainproject.knowledgegraph.control;

import org.humanbrainproject.knowledgegraph.entity.indexing.QualifiedGraphIndexingSpec;
import org.humanbrainproject.knowledgegraph.entity.jsonld.JsonLdEdge;
import org.humanbrainproject.knowledgegraph.entity.jsonld.JsonLdVertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;


public abstract class VertexRepository<T> {

    public static final String UNRESOLVED_LINKS = "http://schema.hbp.eu/propertygraph/unresolved";

    @Autowired
    Configuration configuration;

    protected Logger logger = LoggerFactory.getLogger(VertexRepository.class);


    public abstract List<JsonLdEdge> getEdgesToBeRemoved(JsonLdVertex vertex, T transactionOrConnection);

    private void insertOrUpdateEdges(JsonLdVertex vertex, T transactionOrConnection) {
        for (int i = 0; i < vertex.getEdges().size(); i++) {
            JsonLdEdge edge = vertex.getEdges().get(i);
            if(edge.isEmbedded() || isInternalEdge(edge, configuration.getNexusBase())){
                edge.setOrderNumber(i);
                if(!hasEdge(vertex, edge, transactionOrConnection)){
                    createEdge(vertex, edge, transactionOrConnection);
                }
                else{
                    replaceEdge(vertex, edge, transactionOrConnection);
                }
            }
        }
    }


    public void uploadToPropertyGraph(QualifiedGraphIndexingSpec spec, T transactionOrConnection)  {
        for (JsonLdVertex vertex : spec.getVertices()) {
            if (alreadyExists(vertex, transactionOrConnection)) {
                updateVertex(vertex, transactionOrConnection);
            } else {
                insertVertex(vertex, transactionOrConnection);
            }
            for (JsonLdEdge edge : getEdgesToBeRemoved(vertex, transactionOrConnection)) {
                removeEdge(vertex, edge, transactionOrConnection);
            }
            insertOrUpdateEdges(vertex, transactionOrConnection);
        }
        for (JsonLdVertex vertex : spec.getVertices()) {
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

    protected abstract boolean alreadyExists(JsonLdVertex vertex, T transactionOrConnection);

    protected abstract void updateVertex(JsonLdVertex vertex, T transactionOrConnection) ;

    protected abstract void insertVertex(JsonLdVertex vertex, T transactionOrConnection) ;

    protected abstract boolean hasEdge(JsonLdVertex vertex, JsonLdEdge edge, T transactionOrConnection);

    protected abstract void createEdge(JsonLdVertex vertex, JsonLdEdge edge, T transactionOrConnection);

    protected abstract void replaceEdge(JsonLdVertex vertex, JsonLdEdge edge, T transactionOrConnection);

    protected abstract void removeEdge(JsonLdVertex vertex, JsonLdEdge edge, T transactionOrConnection);

    public abstract void updateUnresolved(JsonLdVertex vertex, T transactionOrConnection);

}
