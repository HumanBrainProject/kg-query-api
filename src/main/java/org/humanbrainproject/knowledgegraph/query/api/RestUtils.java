package org.humanbrainproject.knowledgegraph.query.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.humanbrainproject.knowledgegraph.query.entity.QueryResult;

import java.io.IOException;

public class RestUtils {

    public static QueryResult toJsonResultIfPossible(QueryResult<?> queryResult) {
        if (queryResult.getResults() instanceof String) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode json = mapper.readTree((String)queryResult.getResults());
                QueryResult newResult = new QueryResult();
                newResult.setApiName(queryResult.getApiName());
                newResult.setStart(queryResult.getStart());
                newResult.setSize(queryResult.getSize());
                newResult.setOriginalJson(queryResult.getOriginalJson());
                newResult.setTotal(queryResult.getTotal());
                newResult.setResults(json);
                return newResult;
            } catch (IOException e) {
                return queryResult;
            }
        }
        return queryResult;
    }

}
