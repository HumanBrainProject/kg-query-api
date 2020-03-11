package org.humanbrainproject.knowledgegraph.query.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.humanbrainproject.knowledgegraph.annotations.NoTests;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@NoTests(NoTests.TRIVIAL)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class QueryResult<T> {

    private ExposedDatabaseScope databaseScope;
    private String importantMessage;
    private String apiName;
    private T results;
    private Long total;
    private Long size;
    private Long start;

    public String getImportantMessage() {
        return importantMessage;
    }

    public void setImportantMessage(String importantMessage) {
        this.importantMessage = importantMessage;
    }

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

    public ExposedDatabaseScope getDatabaseScope() {
        return databaseScope;
    }

    public void setDatabaseScope(ExposedDatabaseScope databaseScope) {
        this.databaseScope = databaseScope;
    }

    public static QueryResult<List<Map>> createSingleton(String apiName, Map map){
        QueryResult<List<Map>> result = new QueryResult<>();
        result.setApiName(apiName);
        result.setResults(Collections.singletonList(map));
        result.setStart(0L);
        result.setTotal(1L);
        result.setSize(1L);
        return result;
    }


    public static QueryResult<List<Map>> createEmptyResult(){
        QueryResult<List<Map>> result = new QueryResult<>();
        result.setResults(Collections.emptyList());
        result.setStart(0L);
        result.setTotal(0L);
        result.setSize(0L);
        return result;
    }

}
