package org.humanbrainproject.knowledgegraph.query.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.humanbrainproject.knowledgegraph.annotations.NoTests;

import java.util.List;
import java.util.Map;

@NoTests(NoTests.NO_LOGIC)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TransformedQueryResult<T> extends QueryResult<T>{

    private List<Map> originalJson;

    public List<Map> getOriginalJson() {
        return originalJson;
    }

    public void setOriginalJson(List<Map> originalJson) {
        this.originalJson = originalJson;
    }

}
