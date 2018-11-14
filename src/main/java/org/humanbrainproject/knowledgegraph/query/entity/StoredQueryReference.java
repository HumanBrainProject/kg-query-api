package org.humanbrainproject.knowledgegraph.query.entity;

import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoNamingHelper;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusSchemaReference;

public class StoredQueryReference {

    private final String name;
    private final NexusSchemaReference schemaReference;

    public StoredQueryReference(NexusSchemaReference schemaReference, String name){
        this.schemaReference = schemaReference;
        this.name = schemaReference!=null && schemaReference.getRelativeUrl()!=null ? ArangoNamingHelper.createCompatibleId(schemaReference.getRelativeUrl().getUrl())+"-"+ArangoNamingHelper.createCompatibleId(name) : ArangoNamingHelper.createCompatibleId(name);
    }

    public StoredQueryReference(String name) {
        this(null, name);
    }

    public NexusSchemaReference getSchemaReference() {
        return schemaReference;
    }

    public String getName() {
        return name;
    }
}
