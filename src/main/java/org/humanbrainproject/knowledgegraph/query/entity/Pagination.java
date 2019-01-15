package org.humanbrainproject.knowledgegraph.query.entity;

import org.humanbrainproject.knowledgegraph.annotations.NoTests;

@NoTests(NoTests.TRIVIAL)
public class Pagination {

    private int start=0;
    private Integer size;

    public Pagination setStart(Integer start) {
        this.start = start == null ? 0 : start;
        return this;
    }

    public Pagination setSize(Integer size) {
        this.size = size;
        return this;
    }

    public Integer getSize() {
        return size;
    }

    public int getStart() {
        return start;
    }
}
