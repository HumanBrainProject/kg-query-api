package org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.exceptions;

public class RootCollectionNotFoundException extends RuntimeException{

    public RootCollectionNotFoundException(String s) {
        super(s);
    }
}
