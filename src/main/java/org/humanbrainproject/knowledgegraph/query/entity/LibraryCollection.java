package org.humanbrainproject.knowledgegraph.query.entity;

import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoCollectionReference;

public enum LibraryCollection {

    DATA, META;

    public String getCollectionName(){
        return "library_"+this.name().toLowerCase();
    }

    public ArangoCollectionReference asArangoCollectionReference(){
        return new ArangoCollectionReference(getCollectionName());
    }

}
