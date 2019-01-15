package org.humanbrainproject.knowledgegraph.query.entity;

import org.humanbrainproject.knowledgegraph.testFactory.TestObjectFactory;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class BoundingBoxTest {

    BoundingBox boundingBox;

    @Before
    public void setup(){
        boundingBox = TestObjectFactory.createBoundingBox();
    }

    @Test
    public void parseBoundingBox() {
        String boundingBoxString = "0, 0, 0, 10, 10, 10";
        BoundingBox boundingBox = BoundingBox.parseBoundingBox(boundingBoxString, "refSpace");

        ThreeDVector from = new ThreeDVector(0, 0, 0);
        ThreeDVector to = new ThreeDVector(10, 10, 10);

        assertEquals(from, boundingBox.getFrom());
        assertEquals(to, boundingBox.getTo());
    }


    @Test
    public void parseBoundingBoxAlternative() {
        String boundingBoxString = "[0, 0, 0], [10, 10, 10]";
        BoundingBox boundingBox = BoundingBox.parseBoundingBox(boundingBoxString, "refSpace");

        ThreeDVector from = new ThreeDVector(0, 0, 0);
        ThreeDVector to = new ThreeDVector(10, 10, 10);

        assertEquals(from, boundingBox.getFrom());
        assertEquals(to, boundingBox.getTo());

    }

    @Test
    public void parseBoundingBoxUgly() {
        String boundingBoxString = "0asdf, asdf0,.dsaf asfd0, 10, 10, 10";
        BoundingBox boundingBox = BoundingBox.parseBoundingBox(boundingBoxString, "refSpace");

        ThreeDVector from = new ThreeDVector(0, 0, 0);
        ThreeDVector to = new ThreeDVector(10, 10, 10);

        assertEquals(from, boundingBox.getFrom());
        assertEquals(to, boundingBox.getTo());

    }

    @Test(expected = IllegalArgumentException.class)
    public void parseBoundingBoxNOK() {
        String boundingBoxString = "0, 0, 0, 10, 10";
        BoundingBox.parseBoundingBox(boundingBoxString, "refSpace");
    }
}