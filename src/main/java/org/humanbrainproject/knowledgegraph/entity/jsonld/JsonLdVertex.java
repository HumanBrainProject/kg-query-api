package org.humanbrainproject.knowledgegraph.entity.jsonld;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class JsonLdVertex {
    private String uuid;
    private String id;
    private Integer revision;
    private String type;
    private List<JsonLdEdge> edges = new ArrayList<>();
    private final Set<JsonLdProperty> properties = new LinkedHashSet<>();
    private Boolean deprecated;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Integer getRevision() {
        return revision;
    }

    public void setRevision(Integer revision) {
        this.revision = revision;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
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

}
