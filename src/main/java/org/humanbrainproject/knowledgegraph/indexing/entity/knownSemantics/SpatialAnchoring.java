package org.humanbrainproject.knowledgegraph.indexing.entity.knownSemantics;

import org.humanbrainproject.knowledgegraph.annotations.ToBeTested;
import org.humanbrainproject.knowledgegraph.commons.vocabulary.HBPVocabulary;
import org.humanbrainproject.knowledgegraph.indexing.control.spatial.transformation.QuickNii;
import org.humanbrainproject.knowledgegraph.indexing.control.spatial.transformation.ThreeDTransformation;
import org.humanbrainproject.knowledgegraph.indexing.entity.QualifiedIndexingMessage;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusInstanceReference;
import org.humanbrainproject.knowledgegraph.query.entity.ThreeDVector;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@ToBeTested(easy = true)
public class SpatialAnchoring extends KnownSemantic {

    public enum SpatialAnchoringFormat{
        QuickNii(true), PointCloud(false);
        private final String identifier;
        private final boolean rasterize;
        SpatialAnchoringFormat(boolean rasterize){
            this.identifier = HBPVocabulary.SPATIAL_FORMAT+"/"+name();
            this.rasterize = rasterize;
        }

        public String getIdentifier() {
            return identifier;
        }

        public boolean isRasterize() {
            return rasterize;
        }

        public static SpatialAnchoringFormat byIdentifier(String identifier){
            for (SpatialAnchoringFormat spatialAnchoringFormat : values()) {
                if(spatialAnchoringFormat.getIdentifier().equals(identifier)){
                    return spatialAnchoringFormat;
                }
            }
            return null;
        }
    }

    public SpatialAnchoring(QualifiedIndexingMessage spec) {
        super(spec, HBPVocabulary.SPATIAL_TYPE);
    }

    public NexusInstanceReference getLocatedInstance() {
        return getReferenceForLinkedInstance(spec.getQualifiedMap().get(HBPVocabulary.SPATIAL_LOCATED_INSTANCE), true);
    }

    public SpatialAnchoringFormat getFormat(){
        Object o = spec.getQualifiedMap().get(HBPVocabulary.SPATIAL_FORMAT);
        if(o!=null) {
            return SpatialAnchoringFormat.byIdentifier(o.toString());
        }
        return SpatialAnchoringFormat.PointCloud;
    }

    public String getReferenceSpace() {
        Object o = spec.getQualifiedMap().get(HBPVocabulary.SPATIAL_REFERENCESPACE);
        return o != null ? o.toString() : null;
    }

    public Set<ThreeDVector> getCoordinatesForPointClouds(){
        Object o = spec.getQualifiedMap().get(HBPVocabulary.SPATIAL_COORDINATES);
        if(o instanceof List){
            return ((List<?>)o).stream().filter(c -> c instanceof Map).map(c -> from3ValueCoordinateList((Map) c)).collect(Collectors.toSet());
        }
        else if (o instanceof Map){
            ThreeDVector threeDVector = from3ValueCoordinateList((Map) o);
            if(threeDVector!=null){
                return Collections.singleton(threeDVector);
            }
        }
        return null;
    }

    private ThreeDVector from3ValueCoordinateList(Map coordinate){
        Object x = coordinate.get(HBPVocabulary.SPATIAL_NAMESPACE+"x");
        Object y = coordinate.get(HBPVocabulary.SPATIAL_NAMESPACE+"y");
        Object z = coordinate.get(HBPVocabulary.SPATIAL_NAMESPACE+"z");
        if(x instanceof Number && y instanceof Number && z instanceof Number){
            return new ThreeDVector(((Number)x).doubleValue(), ((Number)y).doubleValue(), ((Number)z).doubleValue());
        }
        return null;
    }



    public ThreeDTransformation getTransformation(){
        Object o = spec.getQualifiedMap().get(HBPVocabulary.SPATIAL_COORDINATES);
        if(o!=null){
            switch (getFormat()) {
                case QuickNii:
                    return new QuickNii((String) o);
            }
        }
        return null;
    }




}
