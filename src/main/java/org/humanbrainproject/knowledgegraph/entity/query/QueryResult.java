package org.humanbrainproject.knowledgegraph.entity.query;

public class QueryResult<T> {

    private String apiName;
    private T result;

    public String getApiName() {
        return apiName;
    }

    public void setApiName(String apiName) {
        this.apiName = apiName;
    }

    public T getResults() {
        return result;
    }

    public void setResults(T result) {
        this.result = result;
    }
}
