package org.humanbrainproject.knowledgegraph.commons.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.humanbrainproject.knowledgegraph.annotations.Tested;
import org.humanbrainproject.knowledgegraph.query.entity.QueryResult;
import org.humanbrainproject.knowledgegraph.query.entity.TransformedQueryResult;

import java.io.IOException;
import java.util.Arrays;

@Tested
public class RestUtils {

    public static final String APPLICATION_LD_JSON = "application/ld+json";
    public static final String APPLICATION_ZIP = "application/zip";

    public static String[] splitCommaSeparatedValues(String original){
        return original!=null ? Arrays.stream(original.split(",")).map(String::trim).toArray(String[]::new) : null;
    }

    public static QueryResult toJsonResultIfPossible(QueryResult<?> queryResult) {
        if (queryResult!=null && queryResult.getResults() instanceof String) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode json = mapper.readTree((String)queryResult.getResults());
                QueryResult newResult;
                if(queryResult instanceof TransformedQueryResult){
                    newResult = new TransformedQueryResult();
                    ((TransformedQueryResult)newResult).setOriginalJson(((TransformedQueryResult) queryResult).getOriginalJson());
                }
                else{
                    newResult = new QueryResult();
                }
                newResult.setApiName(queryResult.getApiName());
                newResult.setStart(queryResult.getStart());
                newResult.setSize(queryResult.getSize());
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
