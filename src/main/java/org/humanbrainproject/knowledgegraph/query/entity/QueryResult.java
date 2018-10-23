package org.humanbrainproject.knowledgegraph.query.entity;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class QueryResult<T> {

    private String apiName;
    private T results;
    private Long total;
    private Long size;
    private Long start;
    private List<Map> originalJson;

    public String getApiName() {
        return apiName;
    }

    public void setApiName(String apiName) {
        this.apiName = apiName;
    }

    public Long getTotal() {
        return total;
    }

    public void setTotal(Long total) {
        this.total = total;
    }

    public T getResults() {
        return results;
    }

    public List<Map> getOriginalJson() {
        return originalJson;
    }

    public void setOriginalJson(List<Map> originalJson) {
        this.originalJson = originalJson;
    }

    public void setResults(T results) {
        this.results = results;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public Long getStart() {
        return start;
    }

    public void setStart(Long start) {
        this.start = start;
    }


}
