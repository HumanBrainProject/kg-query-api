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
