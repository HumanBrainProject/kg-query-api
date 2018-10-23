package org.humanbrainproject.knowledgegraph.indexing.entity;

import org.humanbrainproject.knowledgegraph.propertyGraph.entity.SubSpace;

public interface InstanceReference {

    String getInternalIdentifier();
    InstanceReference toSubSpace(SubSpace subSpace);
    String createUniqueNamespace();
    SubSpace getSubspace();
}
