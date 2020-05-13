/*
 * Copyright 2020 EPFL/Human Brain Project PCO
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.humanbrainproject.knowledgegraph.indexing.control.spatial.transformation;

import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.humanbrainproject.knowledgegraph.annotations.ToBeTested;
import org.humanbrainproject.knowledgegraph.query.entity.ThreeDVector;

import java.util.List;

/**
 * The 3d transformation logic for QuickNii inputs.
 */
@ToBeTested(easy = true)
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
