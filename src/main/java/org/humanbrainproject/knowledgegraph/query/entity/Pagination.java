package org.humanbrainproject.knowledgegraph.query.entity;

public class Pagination {

    private Integer start;
    private Integer size;

    public Pagination setStart(Integer start) {
        this.start = start;
        return this;
    }

    public Pagination setSize(Integer size) {
        this.size = size;
        return this;
    }

    public Integer getSize() {
        return size;
    }

    public Integer getStart() {

        return start;
    }
}
