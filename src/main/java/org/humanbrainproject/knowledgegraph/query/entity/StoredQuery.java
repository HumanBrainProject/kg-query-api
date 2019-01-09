package org.humanbrainproject.knowledgegraph.query.entity;

import org.humanbrainproject.knowledgegraph.annotations.NoTests;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusSchemaReference;

@NoTests(NoTests.TRIVIAL)
public class StoredQuery extends AbstractQuery {

    private final String queryId;
    private String templateId;
    private String libraryId;
    private boolean returnOriginalJson;

    public StoredQuery(NexusSchemaReference schemaReference, String queryId, String vocabulary) {
        super(schemaReference, vocabulary);
        this.queryId = queryId;
    }

    public StoredQueryReference getStoredQueryReference(){
        return queryId!=null ? new StoredQueryReference(getSchemaReference(), queryId) : null;
    }

    public StoredQuery setTemplateId(String templateId){
        this.templateId = templateId;
        return this;
    }

    public StoredTemplateReference getStoredTemplateReference(){
        StoredQueryReference storedQueryReference = getStoredQueryReference();
        return storedQueryReference!=null && templateId!=null ? new StoredTemplateReference(storedQueryReference, templateId) : null;
    }

    public StoredLibraryReference getStoredLibraryReference(){
        return libraryId!=null && templateId!=null ? new StoredLibraryReference(libraryId, templateId) : null;
    }

    public StoredQuery setLibraryId(String libraryId) {
        this.libraryId = libraryId;
        return this;
    }

    public boolean isReturnOriginalJson() {
        return returnOriginalJson;
    }

    public void setReturnOriginalJson(boolean returnOriginalJson) {
        this.returnOriginalJson = returnOriginalJson;
    }
}
