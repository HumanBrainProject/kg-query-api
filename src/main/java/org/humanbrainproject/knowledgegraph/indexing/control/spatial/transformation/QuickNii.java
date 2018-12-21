package org.humanbrainproject.knowledgegraph.indexing.control.spatial.transformation;

import org.apache.commons.math3.linear.*;
import org.humanbrainproject.knowledgegraph.query.entity.ThreeDVector;

import java.util.List;

public class QuickNii implements ThreeDTransformation{

    RealMatrix matrix;


    public QuickNii(String anchoring){
        this(ThreeDVector.parse(anchoring));
    }

    private QuickNii(List<ThreeDVector> vectors){
        this(vectors.get(0), vectors.get(1), vectors.get(2));
    }

    private QuickNii(ThreeDVector o, ThreeDVector u, ThreeDVector v) {
        double[][] matrixData = { {o.getX(),o.getY(),o.getZ()}, {u.getX(), u.getY(), u.getZ()}, {v.getX(), v.getY(), v.getZ()}};
        this.matrix = MatrixUtils.createRealMatrix(matrixData);
    }

    @Override
    public ThreeDVector getPoint(double x, double y){
        RealVector vector  = new ArrayRealVector(new double[]{1,x,y});
        RealVector realVector = matrix.preMultiply(vector);
        return new ThreeDVector(realVector.getEntry(0), realVector.getEntry(1), realVector.getEntry(2));
    }

}
