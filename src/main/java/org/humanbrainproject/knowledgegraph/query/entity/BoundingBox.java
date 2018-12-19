package org.humanbrainproject.knowledgegraph.query.entity;

public class BoundingBox {

    private final Voxel from;
    private final Voxel to;
    private final String referenceSpace;

    public BoundingBox(float xFrom, float yFrom, float zFrom, float xTo, float yTo, float zTo, String referenceSpace){
        this(new Voxel(xFrom, yFrom, zFrom), new Voxel(xTo, yTo, zTo), referenceSpace);
    }

    public static BoundingBox parseBoundingBox(String boundingBox, String referenceSpace) {
        String normalized = boundingBox.replaceAll("[^0-9,.]", "");
        String[] split = normalized.split(",");
        if(split.length!=6){
            throw new IllegalArgumentException("Invalid length of values in bounding box");
        }
        return new BoundingBox(Float.parseFloat(split[0]), Float.parseFloat(split[1]), Float.parseFloat(split[2]), Float.parseFloat(split[3]), Float.parseFloat(split[4]), Float.parseFloat(split[5]), referenceSpace);
    }

    public BoundingBox(Voxel from, Voxel to, String referenceSpace) {
        this.from = from;
        this.to = to;
        this.referenceSpace = referenceSpace;
    }

    public Voxel getFrom() {
        return from;
    }

    public Voxel getTo() {
        return to;
    }

    public String getReferenceSpace() {
        return referenceSpace;
    }
}
