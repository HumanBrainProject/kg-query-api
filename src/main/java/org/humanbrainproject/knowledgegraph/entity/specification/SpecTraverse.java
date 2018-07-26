package org.humanbrainproject.knowledgegraph.entity.specification;

public class SpecTraverse {
    public final String pathName;
    public final boolean reverse;

    public SpecTraverse(String pathName, boolean reverse) {
        this.pathName = pathName;
        this.reverse = reverse;
    }
}
