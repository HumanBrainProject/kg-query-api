package org.humanbrainproject.knowledgegraph.query.entity;

public class StoredLibraryReference {

    private final String name;

    public StoredLibraryReference(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
