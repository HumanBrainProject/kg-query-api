package org.humanbrainproject.knowledgegraph.commons.propertyGraph.entity;

import org.humanbrainproject.knowledgegraph.annotations.NoTests;

@NoTests(NoTests.TRIVIAL)
public enum SubSpace {

    MAIN(""), EDITOR("editor"), INFERRED("inferred"), SUGGESTION("editorsug");

    SubSpace(String postFix){
        this.postFix = postFix;
    }
    private final String postFix;

    public String getPostFix() {
        return postFix;
    }

    public static SubSpace byPostfix(String postFix){
        for (SubSpace subSpace : SubSpace.values()) {
            if(subSpace.getPostFix().equals(postFix)){
                return subSpace;
            }
        }
        return null;
    }

}
