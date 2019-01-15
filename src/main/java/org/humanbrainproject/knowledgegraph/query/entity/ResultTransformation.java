package org.humanbrainproject.knowledgegraph.query.entity;

import org.humanbrainproject.knowledgegraph.annotations.NoTests;

@NoTests(NoTests.NO_LOGIC)
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
