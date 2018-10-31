package org.humanbrainproject.knowledgegraph.commons.nexus.control;

import com.github.jsonldjava.core.JsonLdConsts;
import org.humanbrainproject.knowledgegraph.commons.jsonld.control.JsonTransformer;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.entity.*;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusInstanceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class NexusDocumentConverter {

    @Autowired
    JsonTransformer jsonTransformer;

    @Autowired
    NexusConfiguration configuration;

    protected Logger logger = LoggerFactory.getLogger(NexusDocumentConverter.class);


    private Map<String, Object> createMapFromVertex(Vertex vertex){
        Map<String, Object> jsonObject = new LinkedHashMap<>();
        for (Property property : vertex.getProperties()) {
            jsonObject.put(property.getName(), property.getValue());
        }
        for (Edge edge : vertex.getEdges()) {
            if(edge instanceof EmbeddedEdge){
                jsonObject.put(edge.getName(), createMapFromVertex(((EmbeddedEdge) edge).getToVertex()));
            }
            else if(edge instanceof InternalEdge){
                InternalEdge internalEdge = (InternalEdge) edge;
                Object fromMap = jsonObject.get(edge.getName());
                Map<String, String> referenceMap = new LinkedHashMap<>();
                referenceMap.put(JsonLdConsts.ID, configuration.getAbsoluteUrl(internalEdge.getReference()));
                if(fromMap!=null){
                    if(fromMap instanceof Collection){
                        ((Collection)fromMap).add(referenceMap);
                    }
                    else if (fromMap instanceof Map){
                        List<Map> newcoll = new ArrayList<>();
                        newcoll.add((Map)fromMap);
                        newcoll.add(referenceMap);
                        jsonObject.put(internalEdge.getName(), newcoll);
                    }
                    else{
                        throw new RuntimeException(String.format("Invalid object as a reference in field %s", edge.getName()));
                    }
                }
                else{
                    jsonObject.put(internalEdge.getName(), referenceMap);
                }
            }
        }
        return jsonObject;
    }

    public String createJsonFromVertex(NexusInstanceReference reference, org.humanbrainproject.knowledgegraph.commons.propertyGraph.entity.MainVertex vertex){
        return jsonTransformer.getMapAsJson(createMapFromVertex(vertex));
    }

    private void handleContent(Map<String, Object> parentMap, Vertex vertex){
        for (Property property : vertex.getProperties()) {
            parentMap.put(property.getName(), property.getValue());
        }
        for (Edge edge : vertex.getEdges()) {
            if(edge instanceof org.humanbrainproject.knowledgegraph.commons.propertyGraph.entity.EmbeddedEdge){
                org.humanbrainproject.knowledgegraph.commons.propertyGraph.entity.EmbeddedEdge embeddedEdge = ((org.humanbrainproject.knowledgegraph.commons.propertyGraph.entity.EmbeddedEdge)edge);
                Vertex toVertex = embeddedEdge.getToVertex();
                Map<String, Object> embeddedMap = new LinkedHashMap<>();
                handleContent(embeddedMap, toVertex);
                if(parentMap.containsKey(embeddedEdge.getName())){
                    Object existingInstance = parentMap.get(embeddedEdge.getName());
                    if(existingInstance instanceof Collection){

                    }
                    else{
                        List<Object> list = new ArrayList<>();
                        //parentMap.put(embeddedEdge.getName() );

                    }

                }

                parentMap.put(embeddedEdge.getName(), embeddedMap);
            }
        }

//        if(edge instanceof EmbeddedEdge){
//            EmbeddedEdge embeddedEdge = ((EmbeddedEdge)edge);
//            Vertex toVertex = embeddedEdge.getToVertex();
//
//
//
//
//            Vertex toVertex = ((EmbeddedEdge) edge).getToVertex();
//
//        }

    }

}
