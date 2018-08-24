package org.humanbrainproject.knowledgegraph.entity.query;

public class QueryParameters {

    public Integer start;
    public Integer size;
    public boolean released;
    public String authorizationToken;
    public boolean useContext;
    public String organizations;
    public String library;
    public boolean withOriginalJson;
    public boolean originalData;

    public QueryParameters setOriginalData(boolean originalData) {
        this.originalData = originalData;
        return this;
    }

    public QueryParameters setStart(Integer start) {
        this.start = start;
        return this;
    }

    public QueryParameters setSize(Integer size) {
        this.size = size;
        return this;
    }

    public QueryParameters setReleased(boolean released) {
        this.released = released;
        return this;
    }

    public QueryParameters setAuthorizationToken(String authorizationToken) {
        this.authorizationToken = authorizationToken;
        return this;
    }

    public QueryParameters setUseContext(boolean useContext) {
        this.useContext = useContext;
        return this;
    }

    public QueryParameters setOrganizations(String organizations) {
        this.organizations = organizations;
        return this;
    }

    public QueryParameters setLibrary(String library) {
        this.library = library;
        return this;
    }

    public QueryParameters setWithOriginalJson(boolean withOriginalJson) {
        this.withOriginalJson = withOriginalJson;
        return this;
    }
}
