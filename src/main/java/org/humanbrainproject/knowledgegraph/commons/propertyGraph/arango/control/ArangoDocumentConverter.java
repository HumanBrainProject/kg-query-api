package org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control;

import org.humanbrainproject.knowledgegraph.commons.jsonld.control.JsonTransformer;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoDocumentReference;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.entity.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class ArangoDocumentConverter {

    @Autowired
    JsonTransformer jsonTransformer;


    public String createJsonFromVertexOrEdge(ArangoDocumentReference reference, VertexOrEdge vertexOrEdge){
        Map<String, Object> jsonObject = new LinkedHashMap<>();
        jsonObject.put("_id", reference.getId());
        jsonObject.put("_key", reference.getKey());
        jsonObject.put("_originalId", vertexOrEdge.getMainVertex().getInstanceReference().getFullId());
        if(vertexOrEdge instanceof Edge){
            Vertex fromVertex = ((Edge) vertexOrEdge).getFromVertex();
            jsonObject.put("_name", ((Edge)vertexOrEdge).getName());
            jsonObject.put("_orderNumber", ((Edge)vertexOrEdge).getOrderNumber());
            if(vertexOrEdge instanceof EmbeddedEdge) {
                jsonObject.put("_from", ArangoDocumentReference.fromVertexOrEdgeReference(fromVertex).getId());
                jsonObject.put("_to", ArangoDocumentReference.fromVertexOrEdgeReference(((EmbeddedEdge)vertexOrEdge).getToVertex()).getId());
            }
            else if(vertexOrEdge instanceof InternalEdge){
                jsonObject.put("_from", ArangoDocumentReference.fromVertexOrEdgeReference(fromVertex).getId());
                jsonObject.put("_to", ArangoDocumentReference.fromNexusInstance(((InternalEdge)vertexOrEdge).getReference()).getId());
                //This is a loop - it can happen (e.g. for reconciled instances - so we should ensure this never reaches the database).
                if(jsonObject.get("_from").equals(jsonObject.get("_to"))){
                    return null;
                }
            }
        }
        for (Property property : vertexOrEdge.getProperties()) {
            jsonObject.put(property.getName(), property.getValue());
        }
        for (Edge edge : vertexOrEdge.getEdges()) {
            if(!(edge instanceof EmbeddedEdge)){
                Map<String, Object> referenceMap = new LinkedHashMap<>();
                for (Property property : edge.getProperties()) {
                    referenceMap.put(property.getName(), property.getValue());
                }
                jsonObject.put(edge.getName(), referenceMap);
            }
        }
        return jsonTransformer.getMapAsJson(jsonObject);
    }
}
