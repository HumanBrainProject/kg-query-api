package org.humanbrainproject.knowledgegraph.boundary.indexation;

import com.github.jsonldjava.utils.JsonUtils;
import org.humanbrainproject.knowledgegraph.control.jsonld.JsonLdStandardization;
import org.humanbrainproject.knowledgegraph.control.jsonld.JsonLdToVerticesAndEdges;
import org.humanbrainproject.knowledgegraph.control.releasing.ReleasingController;
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

    private List<JsonLdVertex> prepareAndParsePayload(String payload, GraphIndexationSpec spec) throws IOException, JSONException {
        Object jsonLd = jsonLdStandardization.ensureContext(JsonUtils.fromString(payload), spec.getDefaultNamespace());
        jsonLd = jsonLdStandardization.fullyQualify(jsonLd);
        return jsonLdToVerticesAndEdges.transformFullyQualifiedJsonLdToVerticesAndEdges(JsonUtils.toString(jsonLd), spec);
    }

    public void insertJsonOrJsonLd(String jsonOrJsonLdPayload, GraphIndexationSpec spec) throws IOException, JSONException {
         spec.setRevision(1);
        List<JsonLdVertex> jsonLdVertices = prepareAndParsePayload(jsonOrJsonLdPayload, spec);
        transactionalJsonLdInsertion(jsonLdVertices);
    }

    public void updateJsonOrJsonLd(String jsonOrJsonLdPayload, GraphIndexationSpec spec) throws IOException, JSONException {
        List<JsonLdVertex> jsonLdVertices = prepareAndParsePayload(jsonOrJsonLdPayload, spec);
        transactionalJsonLdUpdate(jsonLdVertices);
    }

    public void delete(String entityName, String id, Integer rev) throws JSONException {
       transactionalJsonLdDeletion(entityName, id, rev);
    }

    abstract void transactionalJsonLdInsertion(List<JsonLdVertex> jsonLdVertices) throws JSONException;
    abstract void transactionalJsonLdUpdate(List<JsonLdVertex> jsonLdVertices) throws JSONException;
    abstract void transactionalJsonLdDeletion(String entityName, String rootId, Integer rootRev) throws JSONException;

    public abstract void clearGraph();


    public static class GraphIndexationSpec{
        private String entityName;
        private String permissionGroup;
        private String id;
        private String jsonOrJsonLdPayload;
        private String defaultNamespace;
        private Integer revision;

        public Integer getRevision() {
            return revision;
        }

        public GraphIndexationSpec setRevision(Integer revision) {
            this.revision = revision;
            return this;
        }

        public String getEntityName() {
            return entityName;
        }

        public GraphIndexationSpec setEntityName(String entityName) {
            this.entityName = entityName;
            return this;
        }

        public String getPermissionGroup() {
            return permissionGroup;
        }

        public GraphIndexationSpec setPermissionGroup(String permissionGroup) {
            this.permissionGroup = permissionGroup;
            return this;
        }

        public String getId() {
            return id;
        }

        public GraphIndexationSpec setId(String id) {
            this.id = id;
            return this;
        }

        public String getJsonOrJsonLdPayload() {
            return jsonOrJsonLdPayload;
        }

        public GraphIndexationSpec setJsonOrJsonLdPayload(String jsonOrJsonLdPayload) {
            this.jsonOrJsonLdPayload = jsonOrJsonLdPayload;
            return this;
        }

        public String getDefaultNamespace() {
            return defaultNamespace;
        }

        public GraphIndexationSpec setDefaultNamespace(String defaultNamespace) {
            this.defaultNamespace = defaultNamespace;
            return this;
        }
    }

}
