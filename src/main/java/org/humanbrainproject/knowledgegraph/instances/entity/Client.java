package org.humanbrainproject.knowledgegraph.instances.entity;

public enum Client {

    editor("editor");

    private String postfix;

    Client(String postfix) {
        this.postfix = postfix;
    }

    public String getPostfix() {
        return postfix;
    }
}
