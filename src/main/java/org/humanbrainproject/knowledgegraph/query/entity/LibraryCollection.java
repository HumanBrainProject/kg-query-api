package org.humanbrainproject.knowledgegraph.query.entity;

import org.humanbrainproject.knowledgegraph.annotations.NoTests;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoCollectionReference;

@NoTests(NoTests.TRIVIAL)
public enum LibraryCollection {

    DATA, META;

    public String getCollectionName(){
        return "library_"+this.name().toLowerCase();
    }

    public ArangoCollectionReference asArangoCollectionReference(){
        return new ArangoCollectionReference(getCollectionName());
    }

}
