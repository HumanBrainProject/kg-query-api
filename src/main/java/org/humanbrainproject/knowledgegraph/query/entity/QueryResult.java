/*
 * Copyright 2020 EPFL/Human Brain Project PCO
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.humanbrainproject.knowledgegraph.query.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.humanbrainproject.knowledgegraph.annotations.NoTests;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@NoTests(NoTests.TRIVIAL)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class QueryResult<T> {

    private String databaseScope;
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

    public String getDatabaseScope() {
        return databaseScope;
    }

    public void setDatabaseScope(String databaseScope) {
        this.databaseScope = databaseScope;
    }

    public static QueryResult<List<Map>> createSingleton(String apiName, Map map, String databaseScope){
        QueryResult<List<Map>> result = new QueryResult<>();
        result.setDatabaseScope(databaseScope);
        result.setApiName(apiName);
        result.setResults(Collections.singletonList(map));
        result.setStart(0L);
        result.setTotal(1L);
        result.setSize(1L);
        return result;
    }


    public static QueryResult<List<Map>> createEmptyResult(String databaseScope){
        QueryResult<List<Map>> result = new QueryResult<>();
        result.setDatabaseScope(databaseScope);
        result.setResults(Collections.emptyList());
        result.setStart(0L);
        result.setTotal(0L);
        result.setSize(0L);
        return result;
    }

}
