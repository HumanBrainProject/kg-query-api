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

package org.humanbrainproject.knowledgegraph.query.entity;

import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoDocumentReference;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusInstanceReference;
import org.humanbrainproject.knowledgegraph.testFactory.TestObjectFactory;
import org.junit.Before;
import org.junit.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

public class AbstractQueryTest {

    AbstractQuery abstractQuery;

    @Before
    public void setup(){
        this.abstractQuery = new AbstractQuery(TestObjectFactory.fooInstanceReference().getNexusSchema(), "foo"){};

    }

    @Test
    public void getInstanceReferencesWhitelistBySingleId() {
        this.abstractQuery.getFilter().restrictToSingleId("bar");

        Set<NexusInstanceReference> instanceReferencesWhitelist = this.abstractQuery.getInstanceReferencesWhitelist();

        assertEquals(1, instanceReferencesWhitelist.size());
        assertEquals("bar", instanceReferencesWhitelist.iterator().next().getId());
    }

    @Test
    public void getInstanceReferencesWhitelistByMultipleIds() {
        this.abstractQuery.getFilter().restrictToIds(new String[]{"foo", "bar"});

        Set<NexusInstanceReference> instanceReferencesWhitelist = this.abstractQuery.getInstanceReferencesWhitelist();

        assertEquals(2, instanceReferencesWhitelist.size());

        Set<String> collect = instanceReferencesWhitelist.stream().map(NexusInstanceReference::getId).collect(Collectors.toSet());

        assertTrue(collect.contains("bar"));
        assertTrue(collect.contains("foo"));
    }


    @Test
    public void getDocumentReferenceWhitelistBySingleId() {
        this.abstractQuery.getFilter().restrictToSingleId("bar");

        Set<ArangoDocumentReference> arangoDocumentReferences = this.abstractQuery.getDocumentReferenceWhitelist();

        assertEquals(1, arangoDocumentReferences.size());
        assertEquals("foo-bar-foobar-v0_0_1/bar", arangoDocumentReferences.iterator().next().getId());
    }


    @Test
    public void getDocumentReferenceWhitelistMultipleIds() {
        this.abstractQuery.getFilter().restrictToIds(new String[]{"foo", "bar"});

        Set<ArangoDocumentReference> arangoDocumentReferences = this.abstractQuery.getDocumentReferenceWhitelist();

        assertEquals(2, arangoDocumentReferences.size());

        Set<String> collect = arangoDocumentReferences.stream().map(ArangoDocumentReference::getId).collect(Collectors.toSet());

        assertTrue(collect.contains("foo-bar-foobar-v0_0_1/bar"));
        assertTrue(collect.contains("foo-bar-foobar-v0_0_1/foo"));


    }
}