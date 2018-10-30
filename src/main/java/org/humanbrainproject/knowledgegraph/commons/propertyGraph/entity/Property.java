package org.humanbrainproject.knowledgegraph.commons.propertyGraph.entity;

import java.util.Map;

public class Property<T> {
    private final String name;
    private final T value;

    private Property(String name, T value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public T getValue() {
        return value;
    }

    public static <T> Property<T> createProperty(String name, T value){
        //A property graph property is not allowed to contain maps
        if(value instanceof Map){
            return null;
        }
        return new Property<T>(name, value);
    }

}
