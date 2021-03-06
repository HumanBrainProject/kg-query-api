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
        String boundingBoxString = "refSpace: 0, 0, 0, 10, 10, 10";
        BoundingBox boundingBox = BoundingBox.parseBoundingBox(boundingBoxString);

        ThreeDVector from = new ThreeDVector(0, 0, 0);
        ThreeDVector to = new ThreeDVector(10, 10, 10);

        assertEquals("refSpace", boundingBox.getReferenceSpace());

        assertEquals(from, boundingBox.getFrom());
        assertEquals(to, boundingBox.getTo());
    }


    @Test
    public void parseBoundingBoxAlternative() {
        String boundingBoxString = "refSpace: [0, 0, 0], [10, 10, 10]";
        BoundingBox boundingBox = BoundingBox.parseBoundingBox(boundingBoxString);

        ThreeDVector from = new ThreeDVector(0, 0, 0);
        ThreeDVector to = new ThreeDVector(10, 10, 10);

        assertEquals("refSpace", boundingBox.getReferenceSpace());
        assertEquals(from, boundingBox.getFrom());
        assertEquals(to, boundingBox.getTo());

    }


    @Test
    public void parseBoundingBoxAlternative2() {
        String boundingBoxString = "refSpace: [[0, 0, 0], [10, 10, 10]]";
        BoundingBox boundingBox = BoundingBox.parseBoundingBox(boundingBoxString);

        ThreeDVector from = new ThreeDVector(0, 0, 0);
        ThreeDVector to = new ThreeDVector(10, 10, 10);

        assertEquals("refSpace", boundingBox.getReferenceSpace());
        assertEquals(from, boundingBox.getFrom());
        assertEquals(to, boundingBox.getTo());

    }

    @Test
    public void parseBoundingBoxUgly() {
        String boundingBoxString = "refSpace: 0asdf, asdf0,.dsaf asfd0, 10, 10, 10";
        BoundingBox boundingBox = BoundingBox.parseBoundingBox(boundingBoxString);

        ThreeDVector from = new ThreeDVector(0, 0, 0);
        ThreeDVector to = new ThreeDVector(10, 10, 10);


        assertEquals("refSpace", boundingBox.getReferenceSpace());
        assertEquals(from, boundingBox.getFrom());
        assertEquals(to, boundingBox.getTo());

    }

    @Test(expected = IllegalArgumentException.class)
    public void parseBoundingBoxNOK() {
        String boundingBoxString = "refSpace: 0, 0, 0, 10, 10";
        BoundingBox.parseBoundingBox(boundingBoxString);
    }

    @Test(expected = IllegalArgumentException.class)
    public void parseBoundingBoxWithoutReferenceSpaceNOK() {
        String boundingBoxString = "0, 0, 0, 10, 10";
        BoundingBox.parseBoundingBox(boundingBoxString);
    }
}