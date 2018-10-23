package org.humanbrainproject.knowledgegraph.jsonld.control;


import com.github.jsonldjava.core.JsonLdConsts;
import org.humanbrainproject.knowledgegraph.indexing.entity.QualifiedIndexingMessage;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusInstanceReference;
import org.humanbrainproject.knowledgegraph.nexus.control.NexusConfiguration;
import org.humanbrainproject.knowledgegraph.propertyGraph.entity.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.Map;

/**
 * This class provides the tools to transform a (previously harmonized {@see org.humanbrainproject.propertygraph.jsonld.control.jsonld.JsonLdStandardization})
 * JSON-LD structure to a data structure of vertices and edges which is understood by property graphs.
 */
@Component
public class JsonLdToVerticesAndEdges {

    protected Logger logger = LoggerFactory.getLogger(JsonLdToVerticesAndEdges.class);

    @Autowired
    NexusConfiguration configuration;

    /**
     * Takes a jsonLdPayload (fully qualified) and transforms it into a vertex including subvertices (for embedded instances) and their outgoing edges and prepared properties.
     */
    public MainVertex transformFullyQualifiedJsonLdToVerticesAndEdges(QualifiedIndexingMessage qualifiedNexusIndexingMessage, SubSpace targetSubSpace) {
        SubSpace originalSubspace = qualifiedNexusIndexingMessage.getOriginalMessage().getInstanceReference().getSubspace();
        targetSubSpace = targetSubSpace == null ? originalSubspace : targetSubSpace;
        MainVertex targetVertex = new MainVertex(qualifiedNexusIndexingMessage.getOriginalMessage().getInstanceReference().toSubSpace(targetSubSpace));
        fillVertexWithProperties(targetVertex, qualifiedNexusIndexingMessage.getQualifiedMap());
        return targetVertex;
    }

    private boolean isInternalEdge(URL url){
        return url.toExternalForm().startsWith(configuration.getNexusEndpoint());
    }

    private void handleSingleElement(Vertex parentVertex, String key, Object element, Integer orderNumber) {
        if(element instanceof Map){
            Map map = (Map)element;
            Edge edge = null;
            Object reference = map.get(JsonLdConsts.ID);
            URL referenceURL;
            try{
                referenceURL = reference!=null ? new URL((String)reference) : null;
            }
            catch (MalformedURLException exception){
                logger.warn("Invalid url as a JSON-LD reference", exception);
                referenceURL = null;
            }
            if(referenceURL!=null){
                if(isInternalEdge(referenceURL)){
                    edge = new InternalEdge(key, parentVertex, NexusInstanceReference.createFromUrl(referenceURL.toExternalForm()), orderNumber);
                }
                else{
                    edge = new ExternalEdge(key, parentVertex, referenceURL, orderNumber);
                }
                for (Object innerKey : map.keySet()) {
                    Property property = Property.createProperty((String)innerKey, map.get(innerKey));
                    if(property!=null){
                        edge.getProperties().add(property);
                    }
                }
            }
            else{
                //It's an embedded instance
                EmbeddedEdge embeddedEdge = new EmbeddedEdge(key, parentVertex, orderNumber);
                EmbeddedVertex embeddedVertex = new EmbeddedVertex(embeddedEdge);
                fillVertexWithProperties(embeddedVertex, map);
                edge = embeddedEdge;
            }
            parentVertex.getEdges().add(edge);
        }
        else{
            //It's a leaf
            Property property = Property.createProperty(key, element);
            if(property!=null) {
                parentVertex.getProperties().add(property);
            }
        }
    }

    private void fillVertexWithProperties(Vertex vertex, Map map){
        for (Object key : map.keySet()) {
            Object element = map.get(key);
            int orderCounter = 0;
            if(element instanceof Collection){
                for (Object el : ((Collection) element)) {
                       handleSingleElement(vertex, (String)key, el, orderCounter++);
                }
            }
            else{
                handleSingleElement(vertex, (String)key, element, orderCounter);
            }
        }
    }

}
