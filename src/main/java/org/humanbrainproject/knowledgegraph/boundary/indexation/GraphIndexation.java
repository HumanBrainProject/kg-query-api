package org.humanbrainproject.knowledgegraph.boundary.indexation;

import com.github.jsonldjava.utils.JsonUtils;
import org.humanbrainproject.knowledgegraph.control.jsonld.JsonLdStandardization;
import org.humanbrainproject.knowledgegraph.control.jsonld.JsonLdToVerticesAndEdges;
import org.humanbrainproject.knowledgegraph.entity.jsonld.JsonLdVertex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.configurationprocessor.json.JSONException;

import java.io.IOException;
import java.util.List;


public abstract class GraphIndexation {


    @Autowired
    JsonLdStandardization jsonLdStandardization;

    @Autowired
    JsonLdToVerticesAndEdges jsonLdToVerticesAndEdges;

    /**
     * Upload the given json or jsonLd payload to the graph. Provide a defaultNamespace that can be used for your JSON or for the not-fully-declared JSON-LD.
     *
     * @param jsonOrJsonLdPayload
     * @param defaultNamespace
     * @throws IOException
     * @throws JSONException
     */
    public void uploadJsonOrJsonLd(String jsonOrJsonLdPayload, String defaultNamespace, String vertexLabel) throws IOException, JSONException {
        Object jsonLd = jsonLdStandardization.ensureContext(JsonUtils.fromString(jsonOrJsonLdPayload), defaultNamespace);
        System.out.println(JsonUtils.toPrettyString(jsonLd));
        jsonLd = jsonLdStandardization.fullyQualify(jsonLd);
        List<JsonLdVertex> jsonLdVertices = jsonLdToVerticesAndEdges.transformFullyQualifiedJsonLdToVerticesAndEdges(JsonUtils.toString(jsonLd), vertexLabel);
        transactionalJsonLdUpload(jsonLdVertices);
    }

    /**
     * Upload the given vertices to the underlying graph database
     * @param vertices
     */
    abstract void transactionalJsonLdUpload(List<JsonLdVertex> vertices) throws JSONException;

    public abstract void clearGraph();

}
