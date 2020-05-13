/*
 * Copyright 2020 EPFL/Human Brain Project PCO
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
