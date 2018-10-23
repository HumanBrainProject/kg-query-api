package org.humanbrainproject.knowledgegraph.query.entity;

public class StoredQueryReference {

    private final String name;

    public StoredQueryReference(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
