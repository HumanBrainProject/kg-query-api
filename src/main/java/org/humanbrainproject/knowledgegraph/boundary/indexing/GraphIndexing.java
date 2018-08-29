package org.humanbrainproject.knowledgegraph.boundary.indexing;

import com.github.jsonldjava.utils.JsonUtils;
import org.humanbrainproject.knowledgegraph.control.jsonld.JsonLdStandardization;
import org.humanbrainproject.knowledgegraph.control.jsonld.JsonLdToVerticesAndEdges;
import org.humanbrainproject.knowledgegraph.entity.jsonld.JsonLdVertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.configurationprocessor.json.JSONException;

import java.io.IOException;
import java.util.List;


public abstract class GraphIndexing {


    @Autowired
    JsonLdStandardization jsonLdStandardization;

    @Autowired
    JsonLdToVerticesAndEdges jsonLdToVerticesAndEdges;

    protected Logger logger = LoggerFactory.getLogger(GraphIndexing.class);

    private List<JsonLdVertex> prepareAndParsePayload(GraphIndexationSpec spec) throws IOException, JSONException {
        Object jsonLd = jsonLdStandardization.ensureContext(JsonUtils.fromString(spec.getJsonOrJsonLdPayload()), spec.getDefaultNamespace());
        jsonLd = jsonLdStandardization.fullyQualify(jsonLd);
        jsonLd = jsonLdStandardization.filterKeysByVocabBlacklists(jsonLd);
        return jsonLdToVerticesAndEdges.transformFullyQualifiedJsonLdToVerticesAndEdges(JsonUtils.toString(jsonLd), spec);
    }

    public void insertJsonOrJsonLd(GraphIndexationSpec spec) throws IOException, JSONException {
         spec.setRevision(1);
        List<JsonLdVertex> jsonLdVertices = prepareAndParsePayload(spec);
        transactionalJsonLdInsertion(jsonLdVertices);
    }

    public void updateJsonOrJsonLd(GraphIndexationSpec spec) throws IOException, JSONException {
        List<JsonLdVertex> jsonLdVertices = prepareAndParsePayload(spec);
        transactionalJsonLdUpdate(jsonLdVertices);
    }

    public void delete(String entityName, String key, Integer rev) {
       transactionalJsonLdDeletion(entityName, key, rev);
    }

    abstract void transactionalJsonLdInsertion(List<JsonLdVertex> jsonLdVertices) throws JSONException;
    abstract void transactionalJsonLdUpdate(List<JsonLdVertex> jsonLdVertices) throws JSONException;
    abstract void transactionalJsonLdDeletion(String entityName, String rootId, Integer rootRev);

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
