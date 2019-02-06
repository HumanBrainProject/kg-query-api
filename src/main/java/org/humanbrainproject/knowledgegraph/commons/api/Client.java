package org.humanbrainproject.knowledgegraph.commons.api;

import org.humanbrainproject.knowledgegraph.annotations.NoTests;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.entity.SubSpace;

@NoTests(NoTests.NO_LOGIC)
public enum Client {

    editor(SubSpace.EDITOR), suggestion(SubSpace.SUGGESTION);

    private SubSpace subSpace;

    Client(SubSpace subSpace) {
        this.subSpace = subSpace;
    }

    public SubSpace getSubSpace() {
        return subSpace;
    }
}
