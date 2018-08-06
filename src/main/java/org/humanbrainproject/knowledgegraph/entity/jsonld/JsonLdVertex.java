package org.humanbrainproject.knowledgegraph.entity.jsonld;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class JsonLdVertex {
    private String entityName;
    private String key;
    private Integer revision;
    private final List<JsonLdEdge> edges = new ArrayList<>();
    private final Set<JsonLdProperty> properties = new LinkedHashSet<>();
    private Boolean deprecated;
    private boolean embedded;

    public String getKey() {
        return key;
    }

    public JsonLdVertex setKey(String key) {
        this.key = key;
        return this;
    }

    public String getEntityName() {
        return entityName;
    }

    public JsonLdVertex setEntityName(String entityName) {
        this.entityName = entityName;
        return this;
    }

    public Integer getRevision() {
        return revision;
    }

    public JsonLdVertex setRevision(Integer revision) {
        this.revision = revision;
        return this;
    }

    public List<JsonLdEdge> getEdges() {
        return edges;
    }

    public Set<JsonLdProperty> getProperties() {
        return properties;
    }

    public Boolean getDeprecated() {
        return deprecated;
    }

    public JsonLdVertex setDeprecated(Boolean deprecated) {
        this.deprecated = deprecated; return this;
    }

    public JsonLdProperty getPropertyByName(String name){
        for (JsonLdProperty property : properties) {
            if(name.equals(property.getName())){
                return property;
            }
        }
        return null;
    }

    public boolean isEmbedded() {
        return embedded;
    }

    public JsonLdVertex setEmbedded(boolean embedded) {
        this.embedded = embedded;
        return this;
    }

    public JsonLdVertex addProperty(JsonLdProperty property){
        this.properties.add(property);
        return this;
    }

    public JsonLdVertex addEdge(JsonLdEdge edge){
        this.edges.add(edge);
        return this;
    }
}
