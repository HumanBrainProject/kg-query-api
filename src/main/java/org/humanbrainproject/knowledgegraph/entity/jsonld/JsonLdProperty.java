package org.humanbrainproject.knowledgegraph.entity.jsonld;

import com.github.jsonldjava.core.JsonLdConsts;

public class JsonLdProperty {
    private String name;
    private Object value;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public boolean isTypeProperty(){
        return getName()!=null && getName().equals(JsonLdConsts.TYPE);
    }
}