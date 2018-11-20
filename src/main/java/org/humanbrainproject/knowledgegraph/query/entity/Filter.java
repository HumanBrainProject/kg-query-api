package org.humanbrainproject.knowledgegraph.query.entity;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class Filter {

    private List<String> restrictToOrganizations;

    private String queryString;

    public void setQueryString(String queryString) {
        this.queryString = queryString;
    }

    public String getQueryString() {
        return queryString;
    }

    public void restrictToOrganizations(String[] whitelistOfOrganizations){
        this.restrictToOrganizations = Collections.unmodifiableList(Arrays.stream(whitelistOfOrganizations).map(String::trim).collect(Collectors.toList()));
    }

    public List<String> getRestrictToOrganizations() {
        return restrictToOrganizations;
    }
}
