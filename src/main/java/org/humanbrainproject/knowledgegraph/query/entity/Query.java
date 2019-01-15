package org.humanbrainproject.knowledgegraph.query.entity;

import org.humanbrainproject.knowledgegraph.annotations.NoTests;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusSchemaReference;

@NoTests(NoTests.NO_LOGIC)
public class Query extends AbstractQuery{

    private final String specification;

    public Query(StoredQuery storedQuery, String payload){
        super(storedQuery.getSchemaReference(), storedQuery.getVocabulary(), storedQuery.getFilter(), storedQuery.getPagination());
        this.specification = payload;
    }

    public Query(String specification, NexusSchemaReference schemaReference, String vocabulary) {
        super(schemaReference, vocabulary);
        this.specification = specification;
    }

    public String getSpecification() {
        return specification;
    }



}
