package org.humanbrainproject.knowledgegraph.control;

public enum SubSpaceName {

    EDITOR("editor"), RECONCILED("reconciled");

    SubSpaceName(String name){
        this.name = name;
    }
    private final String name;

    public String getName() {
        return name;
    }
}
