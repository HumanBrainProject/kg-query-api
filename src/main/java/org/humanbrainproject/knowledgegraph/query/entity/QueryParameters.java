package org.humanbrainproject.knowledgegraph.query.entity;

import org.humanbrainproject.knowledgegraph.authorization.entity.OidcAccessToken;

import java.util.Collections;
import java.util.Map;

public class QueryParameters {
    private final DatabaseScope databaseScope;
    private final Filter filter;
    private final Pagination pagination;
    private final OidcAccessToken authorization;
    private final ResultTransformation resultTransformation;
    private final QueryContext context;
    private final Map<String, String> allParameters;

    public QueryParameters(DatabaseScope databaseScope, Map<String, String> allParameters) {
        this.allParameters = allParameters == null ? Collections.emptyMap() : allParameters;
        this.databaseScope = databaseScope == null ? DatabaseScope.INFERRED : databaseScope;
        this.filter = new Filter();
        this.pagination = new Pagination();
        this.authorization = new OidcAccessToken();
        this.resultTransformation = new ResultTransformation();
        this.context = new QueryContext();

    }

    public Filter filter() {
        return filter;
    }

    public Pagination pagination() {
        return pagination;
    }


    public OidcAccessToken authorization() {
        return authorization;
    }


    public ResultTransformation resultTransformation() {
        return resultTransformation;
    }

    public QueryContext context() {
        return context;
    }


    public Map<String, String> getAllParameters() {
        return allParameters;
    }

    public DatabaseScope databaseScope() {
        return databaseScope;
    }
}
