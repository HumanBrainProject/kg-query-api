package org.humanbrainproject.knowledgegraph.query.entity;

import org.humanbrainproject.knowledgegraph.annotations.NoTests;

@NoTests(NoTests.NO_LOGIC)
public class SpecTraverse {
    public final String pathName;
    public final boolean reverse;
    public boolean isLinkingInstance(){
        return !pathName.matches("http(s){0,1}://.*");
    }


    public SpecTraverse(String pathName, boolean reverse) {
        this.pathName = pathName;
        this.reverse = reverse;
    }
}
