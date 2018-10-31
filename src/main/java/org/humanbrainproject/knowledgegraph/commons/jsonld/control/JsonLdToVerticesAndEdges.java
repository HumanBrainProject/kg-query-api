package org.humanbrainproject.knowledgegraph.commons.jsonld.control;


import com.github.jsonldjava.core.JsonLdConsts;
import org.humanbrainproject.knowledgegraph.commons.nexus.control.NexusConfiguration;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.entity.*;
import org.humanbrainproject.knowledgegraph.indexing.entity.QualifiedIndexingMessage;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusInstanceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
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
    public MainVertex transformFullyQualifiedJsonLdToVerticesAndEdges(QualifiedIndexingMessage qualifiedNexusIndexingMessage) {
        MainVertex targetVertex = new MainVertex(qualifiedNexusIndexingMessage.getOriginalMessage().getInstanceReference());
        fillVertexWithProperties(targetVertex, qualifiedNexusIndexingMessage.getQualifiedMap(), targetVertex);
        return targetVertex;
    }

    private boolean isInternalEdge(URL url) {
        return url.toExternalForm().startsWith(configuration.getNexusBase());
    }

    private void handleSingleElement(Vertex parentVertex, String key, Object element, Integer orderNumber, MainVertex mainVertex) {
        if (element instanceof Map) {
            Map map = (Map) element;
            Edge edge = null;
            Object reference = map.get(JsonLdConsts.ID);
            URL referenceURL;
            try {
                referenceURL = reference != null ? new URL((String) reference) : null;
            } catch (MalformedURLException exception) {
                logger.warn("Invalid url as a JSON-LD reference", exception);
                referenceURL = null;
            }
            if (referenceURL != null && isInternalEdge(referenceURL)) {
                edge = new InternalEdge(key, parentVertex, NexusInstanceReference.createFromUrl(referenceURL.toExternalForm()), orderNumber, mainVertex);
                for (Object innerKey : map.keySet()) {
                    Property property = Property.createProperty((String) innerKey, map.get(innerKey));
                    if (property != null) {
                        edge.getProperties().add(property);
                    }
                }
            } else {
                //It's an embedded instance
                EmbeddedEdge embeddedEdge = new EmbeddedEdge(key, parentVertex, orderNumber, mainVertex);
                EmbeddedVertex embeddedVertex = new EmbeddedVertex(embeddedEdge);
                fillVertexWithProperties(embeddedVertex, map, mainVertex);
                edge = embeddedEdge;
            }
            parentVertex.getEdges().add(edge);
        } else {
            Property property = Property.createProperty(key, element);
            if (property != null) {
                Property propertyByName = parentVertex.getPropertyByName(key);
                if (propertyByName != null) {
                    if (propertyByName.getValue() instanceof Collection) {
                        ((Collection) propertyByName.getValue()).add(property);
                    } else if (propertyByName.getValue() != null) {
                        List<Object> collection = new ArrayList<>();
                        collection.add(propertyByName.getValue());
                        collection.add(property.getValue());
                        parentVertex.getProperties().remove(propertyByName);
                        parentVertex.getProperties().add(Property.createProperty(key, collection));
                    }
                } else {
                    parentVertex.getProperties().add(property);
                }
            }
        }
    }

    private void fillVertexWithProperties(Vertex vertex, Map map, MainVertex mainVertex) {
        for (Object key : map.keySet()) {
            Object element = map.get(key);
            int orderCounter = 0;
            if (element instanceof Collection) {
                for (Object el : ((Collection) element)) {
                    handleSingleElement(vertex, (String) key, el, orderCounter++, mainVertex);
                }
            } else {
                handleSingleElement(vertex, (String) key, element, orderCounter, mainVertex);
            }
        }
    }

}
