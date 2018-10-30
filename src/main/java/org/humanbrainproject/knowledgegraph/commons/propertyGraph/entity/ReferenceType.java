package org.humanbrainproject.knowledgegraph.commons.propertyGraph.entity;

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
