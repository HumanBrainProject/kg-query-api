package org.humanbrainproject.knowledgegraph.propertyGraph.entity;

import java.util.Map;

public class Property {
    private final String name;
    private final Object value;

    private Property(String name, Object value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public Object getValue() {
        return value;
    }

    public static Property createProperty(String name, Object value){
        //A property graph property is not allowed to contain maps
        if(value instanceof Map){
            return null;
        }
        return new Property(name, value);
    }

}
