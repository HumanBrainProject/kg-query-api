package org.humanbrainproject.knowledgegraph.commons.propertyGraph.entity;

import org.humanbrainproject.knowledgegraph.annotations.NoTests;

import java.util.Set;

@NoTests(NoTests.NO_LOGIC)
public class Property {
    private final Object value;
    private final String name;

    private Set<Object> alternatives;

    public Property(String name, Object value) {
        this.name = name;
        this.value = value;
    }

    public Set<Object> getAlternatives() {
        return alternatives;
    }

    public Property setAlternatives(Set<Object> alternatives) {
        this.alternatives = alternatives;
        return this;
    }

    public String getName() {
        return name;
    }

    public Object getValue() {
        return value;
    }

}
