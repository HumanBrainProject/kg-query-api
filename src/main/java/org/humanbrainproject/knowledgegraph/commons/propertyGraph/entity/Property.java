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
        Map<Object, List<Alternative>> m = alternatives.stream().collect(Collectors.groupingBy(Alternative::getValue));
        Set<Alternative> alt = new HashSet<>();
        for(Map.Entry<Object, List<Alternative>> entry : m.entrySet()){
            Set<String> l = entry.getValue().stream().map(Alternative::getUserIds).collect(HashSet::new, Set::addAll, Set::addAll);
            Alternative a = new Alternative(entry.getKey(), l);
            alt.add(a);
        }
        this.alternatives = alt;
        return this;
    }

    public String getName() {
        return name;
    }

    public Object getValue() {
        return value;
    }

}
