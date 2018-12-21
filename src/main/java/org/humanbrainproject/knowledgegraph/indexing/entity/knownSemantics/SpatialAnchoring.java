package org.humanbrainproject.knowledgegraph.indexing.entity.knownSemantics;

import org.humanbrainproject.knowledgegraph.commons.vocabulary.HBPVocabulary;
import org.humanbrainproject.knowledgegraph.indexing.control.spatial.transformation.QuickNii;
import org.humanbrainproject.knowledgegraph.indexing.entity.QualifiedIndexingMessage;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusInstanceReference;

public class SpatialAnchoring extends KnownSemantic {


    public SpatialAnchoring(QualifiedIndexingMessage spec) {
        super(spec, HBPVocabulary.SPATIAL_TYPE);
    }

    public NexusInstanceReference getLocatedInstance() {
        return getReferenceForLinkedInstance(spec.getQualifiedMap().get(HBPVocabulary.SPATIAL_LOCATED_INSTANCE), true);
    }


    public String getReferenceSpace() {
        Object o = spec.getQualifiedMap().get(HBPVocabulary.SPATIAL_REFERENCESPACE);
        return o != null ? o.toString() : null;
    }

    public QuickNii getCoordinates(){
        Object o = spec.getQualifiedMap().get(HBPVocabulary.SPATIAL_COORDINATES);
        if(o!=null){
            return new QuickNii((String) o);
        }
        return null;
    }




}
