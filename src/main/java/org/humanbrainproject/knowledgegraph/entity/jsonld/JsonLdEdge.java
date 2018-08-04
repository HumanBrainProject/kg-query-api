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

    public void setTarget(JsonLdVertex target) {
        this.target = target;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(Integer orderNumber) {
        this.orderNumber = orderNumber;
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
