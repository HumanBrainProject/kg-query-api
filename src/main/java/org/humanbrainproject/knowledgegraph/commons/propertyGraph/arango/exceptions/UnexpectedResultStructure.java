package org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.exceptions;

import org.humanbrainproject.knowledgegraph.annotations.NoTests;

@NoTests(NoTests.NO_LOGIC)
public class UnexpectedResultStructure extends RuntimeException{

    public UnexpectedResultStructure() {
    }

    public UnexpectedResultStructure(String s) {
        super(s);
    }

    public UnexpectedResultStructure(String s, Throwable throwable) {
        super(s, throwable);
    }

    public UnexpectedResultStructure(Throwable throwable) {
        super(throwable);
    }

    public UnexpectedResultStructure(String s, Throwable throwable, boolean b, boolean b1) {
        super(s, throwable, b, b1);
    }
}
