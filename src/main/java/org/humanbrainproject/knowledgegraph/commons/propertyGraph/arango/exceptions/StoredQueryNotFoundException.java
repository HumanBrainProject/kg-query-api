package org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.exceptions;

import org.humanbrainproject.knowledgegraph.annotations.NoTests;

@NoTests(NoTests.NO_LOGIC)
public class StoredQueryNotFoundException extends RuntimeException{

    public StoredQueryNotFoundException(String s) {
        super(s);
    }
}
