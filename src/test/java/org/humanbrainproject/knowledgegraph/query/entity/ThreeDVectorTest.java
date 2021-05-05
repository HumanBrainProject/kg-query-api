/*
 * Copyright 2018 - 2021 Swiss Federal Institute of Technology Lausanne (EPFL)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * This open source software code was developed in part or in whole in the
 * Human Brain Project, funded from the European Union's Horizon 2020
 * Framework Programme for Research and Innovation under
 * Specific Grant Agreements No. 720270, No. 785907, and No. 945539
 * (Human Brain Project SGA1, SGA2 and SGA3).
 *
 */

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