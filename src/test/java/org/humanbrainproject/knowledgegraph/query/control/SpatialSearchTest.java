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

package org.humanbrainproject.knowledgegraph.query.control;

import org.apache.solr.client.solrj.SolrServerException;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoDocumentReference;
import org.humanbrainproject.knowledgegraph.commons.solr.Solr;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public class SpatialSearchTest {

    SpatialSearch spatialSearch;

    @Before
    public void setup(){
        this.spatialSearch = new SpatialSearch();
        this.spatialSearch.solr = Mockito.mock(Solr.class);
    }


    @Test
    public void minimalBoundingBox() throws IOException, SolrServerException {
        Mockito.doReturn(Arrays.asList("foobar/foo", "foobar/bar")).when(this.spatialSearch.solr).queryIdsOfMinimalBoundingBox(Mockito.any());

        Set<ArangoDocumentReference> arangoDocumentReferences = this.spatialSearch.minimalBoundingBox(null);

        Assert.assertEquals(2, arangoDocumentReferences.size());
        Set<String> references = arangoDocumentReferences.stream().map(ArangoDocumentReference::getId).collect(Collectors.toSet());
        Assert.assertTrue(references.contains("foobar/foo"));
        Assert.assertTrue(references.contains("foobar/bar"));
    }
}