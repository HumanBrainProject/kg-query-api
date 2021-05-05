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

package org.humanbrainproject.knowledgegraph.indexing.control;

import org.humanbrainproject.knowledgegraph.commons.jsonld.control.JsonTransformer;
import org.humanbrainproject.knowledgegraph.commons.nexus.control.NexusConfiguration;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.entity.Vertex;
import org.humanbrainproject.knowledgegraph.indexing.entity.IndexingMessage;
import org.humanbrainproject.knowledgegraph.indexing.entity.QualifiedIndexingMessage;
import org.humanbrainproject.knowledgegraph.testFactory.TestObjectFactory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class MessageProcessorTest {


    JsonTransformer json = new JsonTransformer();

    MessageProcessor messageProcessor;
    @Before
    public void setup(){
        messageProcessor  = new MessageProcessor();
        messageProcessor.configuration = Mockito.spy(new NexusConfiguration());
        Mockito.doReturn("http://test/").when(messageProcessor.configuration).getNexusBase();
        Mockito.doReturn("http://test/").when(messageProcessor.configuration).getNexusEndpoint();
    }


    @Test
    public void createVertexStructureWithEdge() {
        String payload = "{'http://test/foo': { '@id': 'http://test/foo/core/bar/v0.0.1/xy'}}";
        QualifiedIndexingMessage qualifiedIndexingMessage = new QualifiedIndexingMessage(new IndexingMessage(TestObjectFactory.fooInstanceReference(), payload, null, null), json.parseToMap(payload));
        Vertex vertexStructure = messageProcessor.createVertexStructure(qualifiedIndexingMessage);
        Assert.assertEquals(1, vertexStructure.getEdges().size());
        Assert.assertEquals("foo/core/bar/v0.0.1/xy", vertexStructure.getEdges().get(0).getReference().getRelativeUrl().getUrl());
    }

    @Test
    public void createVertexStructureWithEdges() {
        String payload = "{'http://test/foo': [{ '@id': 'http://test/foo/core/bar/v0.0.1/xy'}, { '@id': 'http://test/foo/core/bar/v0.0.1/abc'}]}";
        QualifiedIndexingMessage qualifiedIndexingMessage = new QualifiedIndexingMessage(new IndexingMessage(TestObjectFactory.fooInstanceReference(), payload, null, null), json.parseToMap(payload));
        Vertex vertexStructure = messageProcessor.createVertexStructure(qualifiedIndexingMessage);
        Assert.assertEquals(2, vertexStructure.getEdges().size());
        Assert.assertEquals("foo/core/bar/v0.0.1/xy", vertexStructure.getEdges().get(0).getReference().getRelativeUrl().getUrl());
        Assert.assertEquals("foo/core/bar/v0.0.1/abc", vertexStructure.getEdges().get(1).getReference().getRelativeUrl().getUrl());

    }
}