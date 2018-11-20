package org.humanbrainproject.knowledgegraph.instances.entity;

import org.humanbrainproject.knowledgegraph.commons.propertyGraph.entity.SubSpace;

public enum Client {

    editor(SubSpace.EDITOR);

    private SubSpace subSpace;

    Client(SubSpace subSpace) {
        this.subSpace = subSpace;
    }

    public SubSpace getSubSpace() {
        return subSpace;
    }
}
