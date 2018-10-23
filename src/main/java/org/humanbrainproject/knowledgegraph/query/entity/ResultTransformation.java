package org.humanbrainproject.knowledgegraph.query.entity;

public class ResultTransformation {

    private String vocab;

    public ResultTransformation setVocab(String vocab) {
        this.vocab = vocab;
        return this;
    }

    public String getVocab() {
        return vocab;
    }
}
