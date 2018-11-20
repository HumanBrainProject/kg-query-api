package org.humanbrainproject.knowledgegraph.commons.propertyGraph.entity;

public class Tuple<V,T> {
    private final V value1;
    private final T value2;

    public Tuple(V value1, T value2) {
        this.value1 = value1;
        this.value2 = value2;
    }

    public V getValue1() {
        return value1;
    }

    public T getValue2() {
        return value2;
    }
}
