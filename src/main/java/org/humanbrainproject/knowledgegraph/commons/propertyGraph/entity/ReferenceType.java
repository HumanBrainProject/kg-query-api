package org.humanbrainproject.knowledgegraph.commons.propertyGraph.entity;

import org.humanbrainproject.knowledgegraph.annotations.NoTests;

@NoTests(NoTests.NO_LOGIC)
public enum ReferenceType {

    INTERNAL("int"), EMBEDDED("emb");
    private final String prefix;

    ReferenceType(String prefix){
        this.prefix = prefix;
    }

    public String getPrefix() {
        return prefix;
    }
}
