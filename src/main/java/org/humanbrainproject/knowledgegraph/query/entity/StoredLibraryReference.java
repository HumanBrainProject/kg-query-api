package org.humanbrainproject.knowledgegraph.query.entity;

public class StoredLibraryReference {

    private final String name;

    private final LibraryCollection libraryCollection;

    public StoredLibraryReference(String name, LibraryCollection libraryCollection) {
        this.name = name;
        this.libraryCollection = libraryCollection;
    }

    public String getName() {
        return name;
    }

    public LibraryCollection getLibraryCollection() {
        return libraryCollection;
    }
}
