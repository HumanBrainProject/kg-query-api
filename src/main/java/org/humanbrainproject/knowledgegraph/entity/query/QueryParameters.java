package org.humanbrainproject.knowledgegraph.entity.query;

import java.util.Map;

public class QueryParameters {

    public Integer start;
    public Integer size;
    public String searchTerm;
    public boolean released;
    public String authorizationToken;
    public String vocab;
    public boolean useContext;
    public String organizations;
    public String library;
    public boolean withOriginalJson;
    public boolean originalData;
    public Map<String, String> allParameters;



    public QueryParameters setOriginalData(boolean originalData) {
        this.originalData = originalData;
        return this;
    }

    public QueryParameters setAllParameters(Map<String, String> allParameters) {
        this.allParameters = allParameters;
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

    public QueryParameters setVocab(String vocab) {
        this.vocab = vocab;
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

    public QueryParameters setSearchTerm(String searchTerm){
        this.searchTerm = searchTerm;
        return this;
    }
}
