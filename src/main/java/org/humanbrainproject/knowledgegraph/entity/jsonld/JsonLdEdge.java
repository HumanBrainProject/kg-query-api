package org.humanbrainproject.knowledgegraph.entity.jsonld;

import java.util.LinkedHashSet;
import java.util.Set;

public class JsonLdEdge {
    private JsonLdVertex target;
    private String reference;
    private String name;
    private Integer orderNumber;
    private final Set<JsonLdProperty> properties = new LinkedHashSet<>();

    public JsonLdVertex getTarget() {
        return target;
    }

    public JsonLdEdge setTarget(JsonLdVertex target) {
        this.target = target;
        return this;
    }

    public String getReference() {
        return reference;
    }

    public JsonLdEdge setReference(String reference) {
        this.reference = reference;
        return this;
    }

    public String getName() {
        return name;
    }

    public JsonLdEdge setName(String name) {
        this.name = name;
        return this;
    }

    public Integer getOrderNumber() {
        return orderNumber;
    }

    public JsonLdEdge setOrderNumber(Integer orderNumber) {
        this.orderNumber = orderNumber;
        return this;
    }

    public boolean isExternal() {
        return target == null;
    }

    public Set<JsonLdProperty> getProperties() {
        return properties;
    }

    public boolean isEmbedded() {
        return getTarget()!=null;
    }

}
