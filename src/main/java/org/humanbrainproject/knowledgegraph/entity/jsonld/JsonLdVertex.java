package org.humanbrainproject.knowledgegraph.entity.jsonld;

import com.github.jsonldjava.core.JsonLdProcessor;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class JsonLdVertex {
    private String entityName;
    private String id;
    private Integer revision;
    private List<JsonLdEdge> edges = new ArrayList<>();
    private final Set<JsonLdProperty> properties = new LinkedHashSet<>();
    private Boolean deprecated;
    private boolean embedded;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEntityName() {
        return entityName;
    }

    public void setEntityName(String entityName) {
        this.entityName = entityName;
    }

    public Integer getRevision() {
        return revision;
    }

    public void setRevision(Integer revision) {
        this.revision = revision;
    }

    public List<JsonLdEdge> getEdges() {
        return edges;
    }

    public void setEdges(List<JsonLdEdge> edges) {
        this.edges = edges;
    }

    public Set<JsonLdProperty> getProperties() {
        return properties;
    }

    public Boolean getDeprecated() {
        return deprecated;
    }

    public void setDeprecated(Boolean deprecated) {
        this.deprecated = deprecated;
    }

    public JsonLdProperty getTypeProperty(){
        for (JsonLdProperty property : properties) {
            if(property.isTypeProperty()){
                return property;
            }
        }
        return null;
    }

    public boolean isEmbedded() {
        return embedded;
    }

    public void setEmbedded(boolean embedded) {
        this.embedded = embedded;
    }
}
