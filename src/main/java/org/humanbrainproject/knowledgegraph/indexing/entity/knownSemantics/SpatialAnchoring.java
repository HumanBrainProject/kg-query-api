package org.humanbrainproject.knowledgegraph.indexing.entity.knownSemantics;

import org.humanbrainproject.knowledgegraph.commons.vocabulary.HBPVocabulary;
import org.humanbrainproject.knowledgegraph.indexing.entity.QualifiedIndexingMessage;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusInstanceReference;

public class SpatialAnchoring extends KnownSemantic {


    public SpatialAnchoring(QualifiedIndexingMessage spec) {
        super(spec, HBPVocabulary.SPATIAL_TYPE);
    }

    public NexusInstanceReference getLocatedInstance() {
        return getReferenceForLinkedInstance(spec.getQualifiedMap().get(HBPVocabulary.SPATIAL_LOCATED_INSTANCE), true);
    }


    public NexusInstanceReference getReferenceSpace() {
        return getReferenceForLinkedInstance(spec.getQualifiedMap().get(HBPVocabulary.SPATIAL_REFERENCESPACE), true);
    }



}
