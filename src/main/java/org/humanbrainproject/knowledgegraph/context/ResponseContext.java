package org.humanbrainproject.knowledgegraph.context;

import org.humanbrainproject.knowledgegraph.annotations.ToBeTested;
import org.humanbrainproject.knowledgegraph.query.entity.ResultTransformation;
import org.humanbrainproject.knowledgegraph.query.entity.StoredLibraryReference;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

@RequestScope
@Component
@ToBeTested(easy = true)
public class ResponseContext {

    private final ResultTransformation resultTransformation = new ResultTransformation();
    private StoredLibraryReference storedLibraryReference;
    private boolean returnOriginalJson;

    public ResultTransformation getResultTransformation() {
        return resultTransformation;
    }

    public StoredLibraryReference getStoredLibraryReference() {
        return storedLibraryReference;
    }

    public void setStoredLibraryReference(StoredLibraryReference storedLibraryReference) {
        this.storedLibraryReference = storedLibraryReference;
    }

    public boolean isReturnOriginalJson() {
        return returnOriginalJson;
    }

    public void setReturnOriginalJson(boolean returnOriginalJson) {
        this.returnOriginalJson = returnOriginalJson;
    }


    public void populateResponseContext(String vocab) {
        getResultTransformation().setVocab(vocab);
    }


    public void populateResponseContextForTemplating(boolean returnOriginalJson, StoredLibraryReference storedLibraryReference) {
        setReturnOriginalJson(returnOriginalJson);
        setStoredLibraryReference(storedLibraryReference);
    }
}
