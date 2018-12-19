package org.humanbrainproject.knowledgegraph.query.entity;

public class Voxel {

    private final float x;
    private final float y;
    private final float z;

    public Voxel(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Override
    public String toString() {
        return String.format("%.16f,%.16f,%.16f", x, y, z);
    }
}
