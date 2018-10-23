package org.humanbrainproject.knowledgegraph.jsonld.control;

import com.github.jsonldjava.core.JsonLdConsts;
import org.humanbrainproject.knowledgegraph.indexing.entity.QualifiedIndexingMessage;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusInstanceReference;
import org.humanbrainproject.knowledgegraph.nexus.control.NexusConfiguration;
import org.humanbrainproject.knowledgegraph.propertyGraph.entity.*;
import org.humanbrainproject.knowledgegraph.testFactory.TestObjectFactory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;

public class JsonLdToVerticesAndEdgesTest {


    public static final String HTTPS_NEXUSTEST_ORG = "https://nexustest.org";
    private JsonLdToVerticesAndEdges controller;


    @Before
    public void setup(){
        controller = new JsonLdToVerticesAndEdges();
        controller.configuration = Mockito.mock(NexusConfiguration.class);
        Mockito.doReturn(HTTPS_NEXUSTEST_ORG).when(controller.configuration).getNexusEndpoint();
    }


    @Test
    public void transformFullyQualifiedJsonLdToVerticesAndEdges() {
        //given
        Map<String, Object> fullyQualified = new LinkedHashMap<>();
        fullyQualified.put("http://test/foo", "foo");
        QualifiedIndexingMessage indexingMessage = TestObjectFactory.createQualifiedIndexingMessage(TestObjectFactory.fooInstanceReference(), fullyQualified);

        //when
        MainVertex mainVertex = controller.transformFullyQualifiedJsonLdToVerticesAndEdges(indexingMessage, SubSpace.MAIN);

        //then
        Assert.assertEquals(1, mainVertex.getProperties().size());
        Assert.assertEquals("foo", mainVertex.getProperties().get(0).getValue());
        Assert.assertTrue(mainVertex.getEdges().isEmpty());
    }


    @Test
    public void transformFullyQualifiedJsonLdToVerticesAndEdgesEmbedded() {
        //given
        Map<String, Object> fullyQualified = new LinkedHashMap<>();
        fullyQualified.put("http://test/foo", "foo");
        Map<String, Object> embedded = new LinkedHashMap<>();
        fullyQualified.put("http://test/bar", embedded);
        embedded.put("http://test/embedded", "bar");
        QualifiedIndexingMessage indexingMessage = TestObjectFactory.createQualifiedIndexingMessage(TestObjectFactory.fooInstanceReference(), fullyQualified);

        //when
        MainVertex mainVertex = controller.transformFullyQualifiedJsonLdToVerticesAndEdges(indexingMessage, SubSpace.MAIN);

        //then
        Assert.assertEquals(1, mainVertex.getEdges().size());
        Assert.assertEquals(mainVertex, mainVertex.getEdges().get(0).getFromVertex());
        Assert.assertTrue(mainVertex.getEdges().get(0).getProperties().isEmpty());

        Vertex toVertex = ((EmbeddedEdge) mainVertex.getEdges().get(0)).getToVertex();
        Assert.assertNotNull(toVertex);
        Assert.assertEquals(1, toVertex.getProperties().size());
        Assert.assertEquals("http://test/embedded", toVertex.getProperties().get(0).getName());
        Assert.assertEquals("bar", toVertex.getProperties().get(0).getValue());
    }


    @Test
    public void transformFullyQualifiedJsonLdToVerticesAndEdgesMultipleEmbedded() {
        //given
        Map<String, Object> fullyQualified = new LinkedHashMap<>();
        fullyQualified.put("http://test/foo", "foo");
        Map<String, Object> embedded = new LinkedHashMap<>();
        fullyQualified.put("http://test/bar", embedded);
        embedded.put("http://test/embedded", "bar");
        Map<String, Object> embedded2 = new LinkedHashMap<>();
        embedded.put("http://test/deepembed", embedded2);
        embedded2.put("http://test/deepembedvalue", "foobarbar");

        QualifiedIndexingMessage indexingMessage = TestObjectFactory.createQualifiedIndexingMessage(TestObjectFactory.fooInstanceReference(), fullyQualified);

        //when
        MainVertex mainVertex = controller.transformFullyQualifiedJsonLdToVerticesAndEdges(indexingMessage, SubSpace.MAIN);

        //then
        Assert.assertEquals(1, mainVertex.getEdges().size());
        Assert.assertEquals(mainVertex, mainVertex.getEdges().get(0).getFromVertex());
        Assert.assertTrue(mainVertex.getEdges().get(0).getProperties().isEmpty());

        Vertex toVertex = ((EmbeddedEdge) mainVertex.getEdges().get(0)).getToVertex();
        Assert.assertNotNull(toVertex);
        Assert.assertEquals(1, toVertex.getProperties().size());
        Assert.assertEquals("http://test/embedded", toVertex.getProperties().get(0).getName());
        Assert.assertEquals("bar", toVertex.getProperties().get(0).getValue());

        Assert.assertEquals(1, toVertex.getEdges().size());
        Edge deepEmbeddedEdge = toVertex.getEdges().get(0);
        Assert.assertEquals("http://test/deepembed", deepEmbeddedEdge.getName());
        Vertex deepEmbeddedVertex = ((EmbeddedEdge)deepEmbeddedEdge).getToVertex();
        Assert.assertEquals(1, deepEmbeddedVertex.getProperties().size());
        Assert.assertEquals("http://test/deepembedvalue", deepEmbeddedVertex.getProperties().get(0).getName());
        Assert.assertEquals("foobarbar", deepEmbeddedVertex.getProperties().get(0).getValue());
    }

    @Test
    public void transformFullyQualifiedJsonLdToVerticesAndEdgesExternal() {
        //given
        Map<String, Object> fullyQualified = new LinkedHashMap<>();
        fullyQualified.put("http://test/foo", "foo");
        Map<String, Object> externalReference = new LinkedHashMap<>();
        fullyQualified.put("http://test/external", externalReference);
        externalReference.put(JsonLdConsts.ID, "http://foo.com/bar");
        QualifiedIndexingMessage indexingMessage = TestObjectFactory.createQualifiedIndexingMessage(TestObjectFactory.fooInstanceReference(), fullyQualified);

        //when
        MainVertex mainVertex = controller.transformFullyQualifiedJsonLdToVerticesAndEdges(indexingMessage, SubSpace.MAIN);

        //then
        Assert.assertEquals(1, mainVertex.getEdges().size());
        Assert.assertEquals(mainVertex, mainVertex.getEdges().get(0).getFromVertex());
        Assert.assertEquals(1, mainVertex.getEdges().get(0).getProperties().size());
        Assert.assertEquals(JsonLdConsts.ID, mainVertex.getEdges().get(0).getProperties().get(0).getName());


        URL targetUrl = ((ExternalEdge) mainVertex.getEdges().get(0)).getTargetUrl();
        Assert.assertNotNull(targetUrl);
        Assert.assertEquals("http://foo.com/bar", targetUrl.toExternalForm());
    }

    @Test
    public void transformFullyQualifiedJsonLdToVerticesAndEdgesInternal() {
        //given
        Map<String, Object> fullyQualified = new LinkedHashMap<>();
        fullyQualified.put("http://test/foo", "foo");
        Map<String, Object> externalReference = new LinkedHashMap<>();
        fullyQualified.put("http://test/internal", externalReference);
        externalReference.put(JsonLdConsts.ID, HTTPS_NEXUSTEST_ORG+"/v0/data/foo/core/foobar/v0.0.1/bar");
        QualifiedIndexingMessage indexingMessage = TestObjectFactory.createQualifiedIndexingMessage(TestObjectFactory.fooInstanceReference(), fullyQualified);

        //when
        MainVertex mainVertex = controller.transformFullyQualifiedJsonLdToVerticesAndEdges(indexingMessage, SubSpace.MAIN);

        //then
        Assert.assertEquals(1, mainVertex.getEdges().size());
        Assert.assertEquals(mainVertex, mainVertex.getEdges().get(0).getFromVertex());
        Assert.assertEquals(1, mainVertex.getEdges().get(0).getProperties().size());
        Assert.assertEquals(JsonLdConsts.ID, mainVertex.getEdges().get(0).getProperties().get(0).getName());


        NexusInstanceReference reference = ((InternalEdge) mainVertex.getEdges().get(0)).getReference();
        Assert.assertNotNull(reference);
        Assert.assertEquals("foo/core/foobar/v0.0.1/bar", reference.getRelativeUrl());
    }
}