package org.humanbrainproject.knowledgegraph.query.entity;

import org.humanbrainproject.knowledgegraph.annotations.NoTests;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoNamingHelper;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusSchemaReference;

@NoTests(NoTests.TRIVIAL)
public class StoredQueryReference {

    public static final NexusSchemaReference GLOBAL_QUERY_SCHEMA = new NexusSchemaReference("hbpkg", "global", "queries", "v1.0.0");

    private final String name;
    private final NexusSchemaReference schemaReference;
    private final String alias;

    public StoredQueryReference(NexusSchemaReference schemaReference, String name){
        this.schemaReference = schemaReference;
        this.name = schemaReference!=null && schemaReference.getRelativeUrl()!=null ? ArangoNamingHelper.createCompatibleId(schemaReference.getRelativeUrl().getUrl())+"-"+ArangoNamingHelper.createCompatibleId(name) : ArangoNamingHelper.createCompatibleId(name);
        this.alias = name;
    }


    public NexusSchemaReference getSchemaReference() {
        return schemaReference;
    }

    public String getName() {
        return name;
    }

    public String getAlias() {
        return alias;
    }
}
