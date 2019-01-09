package org.humanbrainproject.knowledgegraph.indexing.control.spatial.transformation;


import org.humanbrainproject.knowledgegraph.annotations.NoTests;
import org.humanbrainproject.knowledgegraph.query.entity.ThreeDVector;

@NoTests(NoTests.NO_LOGIC)
public interface ThreeDTransformation {

    ThreeDVector getPoint(double x, double y);

}
