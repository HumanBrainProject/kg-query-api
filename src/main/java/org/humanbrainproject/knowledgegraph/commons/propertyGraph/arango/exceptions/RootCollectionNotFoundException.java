package org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.exceptions;

import org.humanbrainproject.knowledgegraph.annotations.NoTests;

@NoTests(NoTests.NO_LOGIC)
public class RootCollectionNotFoundException extends RuntimeException{

    public RootCollectionNotFoundException(String s) {
        super(s);
    }
}
