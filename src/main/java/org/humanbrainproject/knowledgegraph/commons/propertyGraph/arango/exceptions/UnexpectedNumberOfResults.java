package org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.exceptions;

import org.humanbrainproject.knowledgegraph.annotations.NoTests;

@NoTests(NoTests.NO_LOGIC)
public class UnexpectedNumberOfResults extends RuntimeException{

    public UnexpectedNumberOfResults() {
    }

    public UnexpectedNumberOfResults(String s) {
        super(s);
    }

    public UnexpectedNumberOfResults(String s, Throwable throwable) {
        super(s, throwable);
    }

    public UnexpectedNumberOfResults(Throwable throwable) {
        super(throwable);
    }

    public UnexpectedNumberOfResults(String s, Throwable throwable, boolean b, boolean b1) {
        super(s, throwable, b, b1);
    }
}
