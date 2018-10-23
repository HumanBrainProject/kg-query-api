package deprecated.entity.query;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Set;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class SpatialSearchResult {

    private String id;
    private Set<SpatialCoordinates> coordinates;
    private Set<SpatialLabel> labels;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Set<SpatialCoordinates> getCoordinates() {
        return coordinates;
    }

    public void setCoordinates(Set<SpatialCoordinates> coordinates) {
        this.coordinates = coordinates;
    }

    public Set<SpatialLabel> getLabels() {
        return labels;
    }

    public void setLabels(Set<SpatialLabel> labels) {
        this.labels = labels;
    }

    public static class SpatialCoordinates{

        private String referenceSpace;
        private Float[] coordinate;

        public String getReferenceSpace() {
            return referenceSpace;
        }

        public void setReferenceSpace(String referenceSpace) {
            this.referenceSpace = referenceSpace;
        }

        public Float[] getCoordinate() {
            return coordinate;
        }

        public void setCoordinate(Float[] coordinate) {
            this.coordinate = coordinate;
        }
    }

    public static class SpatialLabel{

        private String labelSpace;
        private String label;

        public String getLabelSpace() {
            return labelSpace;
        }

        public void setLabelSpace(String labelSpace) {
            this.labelSpace = labelSpace;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }
    }
}
