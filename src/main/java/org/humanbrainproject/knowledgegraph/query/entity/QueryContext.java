package org.humanbrainproject.knowledgegraph.query.entity;

public class QueryContext {
    private StoredLibraryReference library;

    private boolean returnOriginalJson;

    public boolean isReturnOriginalJson() {
        return returnOriginalJson;
    }

    public void setReturnOriginalJson(boolean returnOriginalJson) {
        this.returnOriginalJson = returnOriginalJson;
    }

    public StoredLibraryReference getLibrary() {
        return library;
    }

    public QueryContext setLibrary(StoredLibraryReference library) {
        this.library = library;
        return this;
    }
}
