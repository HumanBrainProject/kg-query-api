package org.humanbrainproject.knowledgegraph.entity.indexing;

public class GraphIndexingSpec {
    private String entityName;
    private String permissionGroup;
    private String id;
    private String jsonOrJsonLdPayload;
    private String defaultNamespace;
    private Integer revision;

    public Integer getRevision() {
        return revision != null ? revision : 1;
    }

    public GraphIndexingSpec setRevision(Integer revision) {
        this.revision = revision;
        return this;
    }

    public String getEntityName() {
        return entityName;
    }

    public GraphIndexingSpec setEntityName(String entityName) {
        this.entityName = entityName;
        return this;
    }

    public String getPermissionGroup() {
        return permissionGroup;
    }

    public GraphIndexingSpec setPermissionGroup(String permissionGroup) {
        this.permissionGroup = permissionGroup;
        return this;
    }

    public String getId() {
        return id;
    }

    public GraphIndexingSpec setId(String id) {
        this.id = id;
        return this;
    }

    public String getJsonOrJsonLdPayload() {
        return jsonOrJsonLdPayload;
    }

    public GraphIndexingSpec setJsonOrJsonLdPayload(String jsonOrJsonLdPayload) {
        this.jsonOrJsonLdPayload = jsonOrJsonLdPayload;
        return this;
    }

    public String getDefaultNamespace() {
        return defaultNamespace;
    }

    public GraphIndexingSpec setDefaultNamespace(String defaultNamespace) {
        this.defaultNamespace = defaultNamespace;
        return this;
    }
}
