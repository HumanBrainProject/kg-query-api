package org.humanbrainproject.knowledgegraph.query.entity;

import org.humanbrainproject.knowledgegraph.annotations.Tested;

@Tested
public class StoredTemplateReference {

    private final StoredQueryReference queryReference;
    private final String name;

    public StoredTemplateReference(StoredQueryReference queryReference, String templateId) {
        this.queryReference = queryReference;
        this.name = this.queryReference.getName()+"/"+templateId;
    }

    public StoredQueryReference getQueryReference(){
        return this.queryReference;
    }

    public String getName() {
        return this.name;
    }
}
