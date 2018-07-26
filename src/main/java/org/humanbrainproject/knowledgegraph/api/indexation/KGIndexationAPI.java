package org.humanbrainproject.knowledgegraph.api.indexation;

import java.util.List;

public interface KGIndexationAPI {


    /**
     * Save the payload to the property graph. It is assumed, that the payload is either a valid JSON or JSON-LD structure
     * @param payload - JSON or JSON-LD
     * @param defaultNamespace - optional: Used as the default namespace if some or all keys are not semantically defined.
     * @param vertexLabel - optional: Used to define the label of the vertex / collection in which the document shall be stored.
     * @throws Exception
     */
    void uploadToPropertyGraph(String payload, String defaultNamespace, String vertexLabel) throws Exception;


    /**
     * Wipes all instances of the graph
     */
    void clearGraph();
}
