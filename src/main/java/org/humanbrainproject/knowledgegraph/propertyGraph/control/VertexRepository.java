package org.humanbrainproject.knowledgegraph.propertyGraph.control;

import org.humanbrainproject.knowledgegraph.nexus.control.NexusConfiguration;
import org.humanbrainproject.knowledgegraph.propertyGraph.entity.MainVertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;


public abstract class VertexRepository<Connection extends DatabaseConnection<?>, InternalDocumentReference> {

    public static final String UNRESOLVED_LINKS = "http://schema.hbp.eu/propertygraph/unresolved";

    @Autowired
    NexusConfiguration nexusConfiguration;

    protected Logger logger = LoggerFactory.getLogger(VertexRepository.class);

//
//    public abstract List<JsonLdEdge> getEdgesToBeRemoved(JsonLdVertex vertex, T transactionOrConnection);

    public abstract MainVertex getVertexStructureById(InternalDocumentReference internalDocumentReference, Connection connection);

    public abstract void clearDatabase(Connection connection);

//
//    public void insertOrUpdateEdges(JsonLdVertex vertex, T transactionOrConnection) {
//        for (int i = 0; i < vertex.getEdges().size(); i++) {
//            JsonLdEdge edge = vertex.getEdges().get(i);
//            if(edge.isEmbedded() || isInternalEdge(edge, nexusConfiguration.getNexusBase())){
//                edge.setOrderNumber(i);
//                if(!hasEdge(vertex, edge, transactionOrConnection)){
//                    createEdge(vertex, edge, transactionOrConnection);
//                }
//                else{
//                    replaceEdge(vertex, edge, transactionOrConnection);
//                }
//            }
//        }
//    }
//
//    public void uploadToPropertyGraph(JsonLdVertex vertex, T transactionOrConnection){
//        if (alreadyExists(vertex, transactionOrConnection)) {
//            updateVertex(vertex, transactionOrConnection);
//        } else {
//            insertVertex(vertex, transactionOrConnection);
//        }
//        for (JsonLdEdge edge : getEdgesToBeRemoved(vertex, transactionOrConnection)) {
//            removeEdge(vertex, edge, transactionOrConnection);
//        }
//        insertOrUpdateEdges(vertex, transactionOrConnection);
//        for (JsonLdVertex subVertex : vertex.getEmbeddedVertices()) {
//            uploadToPropertyGraph(subVertex, transactionOrConnection);
//        }
//
//    }
//
//
//    public void uploadToPropertyGraph(ResolvedVertexStructure vertexStructure, T transactionOrConnection)  {
//        uploadToPropertyGraph(vertexStructure.getMainVertex(), transactionOrConnection);
//    }
//
//
//    protected boolean isInternalEdge(JsonLdEdge edge, String nexusBase){
//        if(edge.getReference()==null || edge.getReference().trim().isEmpty() || !edge.getReference().startsWith(nexusBase)){
//            return false;
//        }
//        return true;
//    }
//
//    protected abstract boolean alreadyExists(JsonLdVertex vertex, T transactionOrConnection);
//
//    protected abstract void updateVertex(JsonLdVertex vertex, T transactionOrConnection) ;
//
//    protected abstract void insertVertex(JsonLdVertex vertex, T transactionOrConnection) ;
//
//    protected abstract boolean hasEdge(JsonLdVertex vertex, JsonLdEdge edge, T transactionOrConnection);
//
//    protected abstract void createEdge(JsonLdVertex vertex, JsonLdEdge edge, T transactionOrConnection);
//
//    protected abstract void replaceEdge(JsonLdVertex vertex, JsonLdEdge edge, T transactionOrConnection);
//
//    protected abstract void removeEdge(JsonLdVertex vertex, JsonLdEdge edge, T transactionOrConnection);

}
