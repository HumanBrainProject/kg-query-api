package org.humanbrainproject.knowledgegraph.api.query;

import java.util.List;

public interface KGQueryAPI {

    /**
     * Allows to execute a query on the property graph based on the query specification passed as an argument
     *
     * @param payload - a JSON-LD document following the definition of the kg-query specification
     * @throws Exception
     */
    List<Object> queryPropertyGraphBySpecification(String payload) throws Exception;
}
