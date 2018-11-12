package org.humanbrainproject.knowledgegraph.query.entity;

import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusSchemaReference;

public class StoredQueryReference {

    private final String name;

    public StoredQueryReference(NexusSchemaReference schemaReference, String name){
        this.name = schemaReference.getRelativeUrl()!=null ? schemaReference.getRelativeUrl().getUrl()+"/"+name : name;
    }

    public StoredQueryReference(String name) {
        this(null, name);
    }

    public String getName() {
        return name;
    }
}
