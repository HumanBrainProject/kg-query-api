package org.humanbrainproject.knowledgegraph.query.entity;

import org.humanbrainproject.knowledgegraph.annotations.NoTests;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@NoTests(NoTests.TRIVIAL)
public class Filter {

    private List<String> restrictToOrganizations;
    private List<String> restrictToIds;

    private String queryString;

    public Filter setQueryString(String queryString) {
        this.queryString = queryString;
        return this;
    }

    public String getQueryString() {
        return queryString;
    }

    public Filter restrictToOrganizations(String[] whitelistOfOrganizations) {
        this.restrictToOrganizations = whitelistOfOrganizations == null ? null : Collections.unmodifiableList(Arrays.stream(whitelistOfOrganizations).map(String::trim).collect(Collectors.toList()));

        return this;
    }

    public Filter restrictToIds(String[] ids) {
        this.restrictToIds = ids == null ? null : Collections.unmodifiableList(Arrays.stream(ids).map(String::trim).collect(Collectors.toList()));
        return this;
    }

    public Filter restrictToSingleId(String id) {
        this.restrictToIds = id == null ? null : Collections.singletonList(id.trim());
        return this;
    }

    public List<String> getRestrictToOrganizations() {
        return restrictToOrganizations;
    }

    public List<String> getRestrictToIds() {
        return restrictToIds;
    }



}
