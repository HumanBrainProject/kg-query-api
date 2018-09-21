package org.humanbrainproject.knowledgegraph.entity.indexing;

import java.util.List;

public class SpatialAnchoring extends GraphEntity{

    private static final String SPATIAL_NAMESPACE = "http://hbp.eu/spatial/";
    private static final String SPATIAL_TYPE = String.format("%s%s", SPATIAL_NAMESPACE, "SpatialAnchoring");
    private static final String SPATIAL_COORDINATES = String.format("%s%s", SPATIAL_NAMESPACE, "coordinates");
    private static final String SPATIAL_REFERENCESPACE = String.format("%s%s", SPATIAL_NAMESPACE, "referenceSpace");
    private static final String SPATIAL_LOCATES = String.format("%s%s", SPATIAL_NAMESPACE, "locates");

    public SpatialAnchoring(QualifiedGraphIndexingSpec spec) {
        super(spec, SPATIAL_TYPE);
    }

    public String getLocatedInstance() {
        return getReferenceForLinkedInstance(spec.getMap().get(SPATIAL_LOCATES), true);
    }


    public String getReferenceSpace() {
        return getReferenceForLinkedInstance(spec.getMap().get(SPATIAL_REFERENCESPACE), true);
    }

    public List getCoordinates() {
        return getValueListForProperty(spec.getMap(), SPATIAL_COORDINATES);
    }



}
