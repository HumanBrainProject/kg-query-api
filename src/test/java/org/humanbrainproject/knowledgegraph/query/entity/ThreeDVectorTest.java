package org.humanbrainproject.knowledgegraph.query.entity;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class ThreeDVectorTest {

    @Test
    public void testToString() {
        ThreeDVector threeDVector = new ThreeDVector(0, 10, 20);

        String string = threeDVector.toString();

        assertEquals("0.0000000000000000,10.0000000000000000,20.0000000000000000", string);
    }


    @Test
    public void normalize() {

        ThreeDVector threeDVector = new ThreeDVector(0, 10, 20);

        String normalized = threeDVector.normalize(-10, 10);

        assertEquals("-10.0000000000000000,-8.0000000000000000,-6.0000000000000000", normalized);
    }

    @Test
    public void normalize2() {

        ThreeDVector threeDVector = new ThreeDVector(0, 10, 100);

        String normalized = threeDVector.normalize(-10, 10);

        assertEquals("-10.0000000000000000,-8.0000000000000000,10.0000000000000000", normalized);
    }

    @Test
    public void parse() {

        List<ThreeDVector> parse = ThreeDVector.parse("0,1,2,3,4,5");

        assertEquals(2, parse.size());
        assertTrue(parse.get(0).getX() == 0);
        assertTrue(parse.get(0).getY() == 1);
        assertTrue(parse.get(0).getZ() == 2);
        assertTrue(parse.get(1).getX() == 3);
        assertTrue(parse.get(1).getY() == 4);
        assertTrue(parse.get(1).getZ() == 5);

    }


    @Test
    public void parseIncomplete() {
        List<ThreeDVector> parse = ThreeDVector.parse("0,1,2,3,4");
        assertEquals(1, parse.size());
        assertTrue(parse.get(0).getX() == 0);
        assertTrue(parse.get(0).getY() == 1);
        assertTrue(parse.get(0).getZ() == 2);
    }

    @Test
    public void parseWithCharacters() {
        List<ThreeDVector> parse = ThreeDVector.parse("0,ads1 ,dsf2sdf");
        assertEquals(1, parse.size());
        assertTrue(parse.get(0).getX() == 0);
        assertTrue(parse.get(0).getY() == 1);
        assertTrue(parse.get(0).getZ() == 2);
    }
}