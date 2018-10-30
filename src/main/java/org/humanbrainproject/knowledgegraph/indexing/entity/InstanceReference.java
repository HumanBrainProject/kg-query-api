package org.humanbrainproject.knowledgegraph.indexing.entity;

import org.humanbrainproject.knowledgegraph.commons.propertyGraph.entity.SubSpace;

public interface InstanceReference {
    String getId();
    String getTypeName();
    /**
     * Returns the full id (e.g. combining the typename and the id )
     */
    String getFullId();
    InstanceReference toSubSpace(SubSpace subSpace);
    String createUniqueNamespace();
    SubSpace getSubspace();


}
