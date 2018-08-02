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

    private List<JsonLdVertex> prepareAndParsePayload(String payload, String entityName, String permissionGroup, String defaultNamespace, String id, Integer revision) throws IOException, JSONException {
        Object jsonLd = jsonLdStandardization.ensureContext(JsonUtils.fromString(payload), defaultNamespace);
        jsonLd = jsonLdStandardization.fullyQualify(jsonLd);
        return jsonLdToVerticesAndEdges.transformFullyQualifiedJsonLdToVerticesAndEdges(JsonUtils.toString(jsonLd), entityName, permissionGroup, id, revision);
    }

    public void insertJsonOrJsonLd(String entityName, String permissionGroup, String rootId, String jsonOrJsonLdPayload, String defaultNamespace) throws IOException, JSONException {
         transactionalJsonLdInsertion(prepareAndParsePayload(jsonOrJsonLdPayload, entityName,permissionGroup, defaultNamespace, rootId, 1));
    }

    public void updateJsonOrJsonLd(String entityName, String permissionGroup, String rootId, Integer rootRev, String jsonOrJsonLdPayload,  String defaultNamespace) throws IOException, JSONException {
        transactionalJsonLdUpdate(prepareAndParsePayload(jsonOrJsonLdPayload, entityName,permissionGroup, defaultNamespace, rootId, rootRev));
    }

    public void delete(String entityName, String id, Integer rev) throws JSONException {
       transactionalJsonLdDeletion(entityName, id, rev);
    }

    abstract void transactionalJsonLdInsertion(List<JsonLdVertex> jsonLdVertices) throws JSONException;
    abstract void transactionalJsonLdUpdate(List<JsonLdVertex> jsonLdVertices) throws JSONException;
    abstract void transactionalJsonLdDeletion(String entityName, String rootId, Integer rootRev) throws JSONException;

    public abstract void clearGraph();

}
