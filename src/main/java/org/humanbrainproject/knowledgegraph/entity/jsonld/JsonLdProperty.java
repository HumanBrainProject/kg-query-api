package org.humanbrainproject.knowledgegraph.entity.jsonld;

import com.github.jsonldjava.core.JsonLdConsts;

public class JsonLdProperty {
    private String name;
    private Object value;

    public String getName() {
        return name;
    }

    public JsonLdProperty setName(String name) {
        this.name = name;
        return this;
    }

    public Object getValue() {
        return value;
    }

    public JsonLdProperty setValue(Object value) {
        this.value = value;
        return this;
    }

    public boolean isTypeProperty(){
        return getName()!=null && getName().equals(JsonLdConsts.TYPE);
    }
}