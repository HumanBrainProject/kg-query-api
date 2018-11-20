package org.humanbrainproject.knowledgegraph.query.entity;

public class SpecTraverse {
    public final String pathName;
    public final boolean reverse;

    public SpecTraverse(String pathName, boolean reverse) {
        this.pathName = pathName;
        this.reverse = reverse;
    }
}
