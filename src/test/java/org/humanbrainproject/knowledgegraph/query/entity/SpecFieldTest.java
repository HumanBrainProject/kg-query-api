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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

public class SpecFieldTest {

    SpecField specField;

    @Before
    public void setUp() throws Exception {
        SpecField subField1 = TestObjectFactory.simpleSpecFieldWithOutgoingTraverses("foo", "http://foo");
        SpecField subField2 = TestObjectFactory.simpleSpecFieldWithOutgoingTraverses("bar", "http://bar");
        this.specField = new SpecField("foobar", Arrays.asList(subField1, subField2), Collections.singletonList(new SpecTraverse("http://foobar", false)), null, false, false, false, false, null);
    }


    @Test
    public void isMerge() {
        SpecField subField1 = TestObjectFactory.simpleSpecFieldWithOutgoingTraverses("foo", "http://foo");
        SpecField field = new SpecField("foobar", Collections.singletonList(subField1), null, null, false, false, false, false, null);

        boolean merge = field.isMerge();

        assertTrue(merge);
    }

    @Test
    public void isMergeFalse() {
        boolean merge = this.specField.isMerge();
        assertFalse(merge);
    }

    @Test
    public void needsTraversal() {
        boolean needsTraversal = this.specField.needsTraversal();
        assertTrue(needsTraversal);
    }


    @Test
    public void needsTraversalForInstanceWithoutFieldsButMultipleTraverse() {
        SpecField field = new SpecField("foobar", null, Arrays.asList(new SpecTraverse("http://foobar", false), new SpecTraverse("http://barfoo", false)), null, false, false, false, false, null);
        boolean needsTraversal = field.needsTraversal();
        assertTrue(needsTraversal);
    }

    @Test
    public void needsTraversalFalseForSingleTraverse() {
        SpecField field = new SpecField("foobar", null, Collections.singletonList(new SpecTraverse("http://foobar", false)), null, false, false, false, false, null);
        boolean needsTraversal = field.needsTraversal();
        assertFalse(needsTraversal);
    }

    @Test
    public void getFirstTraversal() {
        SpecField field = new SpecField("foobar", null, Arrays.asList(new SpecTraverse("http://foobar", false), new SpecTraverse("http://barfoo", false)), null, false, false, false, false, null);
        SpecTraverse firstTraversal = field.getFirstTraversal();
        assertEquals("http://foobar", firstTraversal.pathName);
    }

    @Test
    public void getFirstTraversalNull() {
        SpecField field = new SpecField("foobar", null, null, null, false, false, false, false, null);
        SpecTraverse firstTraversal = field.getFirstTraversal();
        assertNull(firstTraversal);
    }

    @Test
    public void getAdditionalDirectTraversals() {
        SpecField field = new SpecField("foobar", Collections.singletonList(TestObjectFactory.simpleSpecFieldWithOutgoingTraverses("foo", "http://foo")), Arrays.asList(new SpecTraverse("http://foobar", false), new SpecTraverse("http://barfoo", false)), null, false, false, false, false, null);
        assertFalse(field.isLeaf());
        List<SpecTraverse> additionalDirectTraversals = field.getAdditionalDirectTraversals();

        assertEquals(1, additionalDirectTraversals.size());
        assertEquals("http://barfoo", additionalDirectTraversals.get(0).pathName);
    }

    @Test
    public void getLeafPath() {
        SpecField field = new SpecField("foobar", null, Arrays.asList(new SpecTraverse("http://foobar", false), new SpecTraverse("http://barfoo", false)), null, false, false, false, false, null);
        assertTrue(field.isLeaf());

        SpecTraverse leafPath = field.getLeafPath();

        assertEquals("http://barfoo", leafPath.pathName);
    }

    @Test
    public void getLeafPathForNonLeaf() {
        SpecField field = new SpecField("foobar", Collections.singletonList(TestObjectFactory.simpleSpecFieldWithOutgoingTraverses("foo", "http://foo")), Arrays.asList(new SpecTraverse("http://foobar", false), new SpecTraverse("http://barfoo", false)), null, false, false, false, false, null);
        assertFalse(field.isLeaf());

        SpecTraverse leafPath = field.getLeafPath();

        assertNull(leafPath);
    }


    @Test
    public void numberOfDirectTraversalsForNonLeaf() {
        SpecField field = new SpecField("foobar", Collections.singletonList(TestObjectFactory.simpleSpecFieldWithOutgoingTraverses("foo", "http://foo")), Arrays.asList(new SpecTraverse("http://foobar", false), new SpecTraverse("http://barfoo", false)), null, false, false, false, false, null);
        assertFalse(field.isLeaf());

        int numberOfDirectTraversals = field.numberOfDirectTraversals();

        assertEquals(2, numberOfDirectTraversals);
    }

    @Test
    public void numberOfDirectTraversals() {

        SpecField field = new SpecField("foobar", null, Arrays.asList(new SpecTraverse("http://foobar", false), new SpecTraverse("http://barfoo", false)), null, false, false, false, false, null);
        assertTrue(field.isLeaf());

        int numberOfDirectTraversals = field.numberOfDirectTraversals();

        assertEquals(1, numberOfDirectTraversals);
    }

    @Test
    public void numberOfDirectTraversalsSingleTraversal() {

        SpecField field = new SpecField("foobar", null, Arrays.asList(new SpecTraverse("http://foobar", false)), null, false, false, false, false, null);
        assertTrue(field.isLeaf());

        int numberOfDirectTraversals = field.numberOfDirectTraversals();

        assertEquals(0, numberOfDirectTraversals);
    }

    @Test
    public void hasNestedGrouping() {

        SpecField nestedField = new SpecField("foo", null,
                Collections.singletonList(new SpecTraverse("http://foo", false)), null, false, false, true, false, null);

        SpecField field = new SpecField("foobar",
                Collections.singletonList(nestedField),
                Collections.singletonList(new SpecTraverse("http://foobar", false)), "http://foo", false, false, false, false, null);

        boolean hasNestedGrouping = field.hasNestedGrouping();

        assertTrue(hasNestedGrouping);


    }
}