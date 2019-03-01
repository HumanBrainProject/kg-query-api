package org.humanbrainproject.knowledgegraph.commons.propertyGraph.entity;

import org.humanbrainproject.knowledgegraph.annotations.NoTests;
import org.humanbrainproject.knowledgegraph.indexing.entity.Alternative;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@NoTests(NoTests.NO_LOGIC)
public class Property {
    private final Object value;
    private final String name;

    private Set<Alternative> alternatives;

    public Property(String name, Object value) {
        this.name = name;
        this.value = value;
    }

    public Set<Alternative> getAlternatives() {
        return alternatives;
    }

    public Property setAlternatives(Set<Alternative> alternatives) {
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
