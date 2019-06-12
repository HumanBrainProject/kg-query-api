package org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.exceptions;

import org.humanbrainproject.knowledgegraph.annotations.NoTests;

@NoTests(NoTests.NO_LOGIC)
public class IllegalDatabaseScope extends RuntimeException{

    public IllegalDatabaseScope(String s) {
        super(s);
    }
}
