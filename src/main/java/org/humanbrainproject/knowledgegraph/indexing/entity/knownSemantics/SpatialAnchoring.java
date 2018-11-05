package org.humanbrainproject.knowledgegraph.indexing.entity.knownSemantics;

import org.humanbrainproject.knowledgegraph.indexing.entity.KnownSemantic;
import org.humanbrainproject.knowledgegraph.indexing.entity.QualifiedIndexingMessage;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusInstanceReference;

public class SpatialAnchoring extends KnownSemantic {

    private static final String SPATIAL_NAMESPACE = "http://hbp.eu/spatial/";
    private static final String SPATIAL_TYPE = String.format("%s%s", SPATIAL_NAMESPACE, "SpatialAnchoring");
    private static final String SPATIAL_COORDINATES = String.format("%s%s", SPATIAL_NAMESPACE, "coordinates");
    private static final String SPATIAL_REFERENCESPACE = String.format("%s%s", SPATIAL_NAMESPACE, "referenceSpace");
    private static final String SPATIAL_LOCATES = String.format("%s%s", SPATIAL_NAMESPACE, "locates");

    public SpatialAnchoring(QualifiedIndexingMessage spec) {
        super(spec, SPATIAL_TYPE);
    }

    public NexusInstanceReference getLocatedInstance() {
        return getReferenceForLinkedInstance(spec.getQualifiedMap().get(SPATIAL_LOCATES), true);
    }


    public NexusInstanceReference getReferenceSpace() {
        return getReferenceForLinkedInstance(spec.getQualifiedMap().get(SPATIAL_REFERENCESPACE), true);
    }



}
