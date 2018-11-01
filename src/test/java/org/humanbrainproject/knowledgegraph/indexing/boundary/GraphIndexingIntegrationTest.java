package org.humanbrainproject.knowledgegraph.indexing.boundary;

import com.github.jsonldjava.core.JsonLdConsts;
import org.humanbrainproject.knowledgegraph.commons.nexus.control.NexusConfiguration;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.entity.SubSpace;
import org.humanbrainproject.knowledgegraph.commons.vocabulary.HBPVocabulary;
import org.humanbrainproject.knowledgegraph.indexing.entity.IndexingMessage;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusInstanceReference;
import org.humanbrainproject.knowledgegraph.testFactory.TestObjectFactory;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

@SpringBootTest
@RunWith(SpringRunner.class)
@Ignore("IntegrationTest")
public class GraphIndexingIntegrationTest {

    @Autowired
    GraphIndexing graphIndexing;

    @Autowired
    NexusConfiguration configuration;

    @Test
    public void insert() throws IOException {
        Map<String, Object> fullyQualified = new LinkedHashMap<>();
        fullyQualified.put("http://test/foo", "foo");
        NexusInstanceReference instanceReference = TestObjectFactory.fooInstanceReference();
        IndexingMessage message = TestObjectFactory.createIndexingMessage(instanceReference, fullyQualified);
        graphIndexing.insert(message);

        //cleanup
        graphIndexing.delete(instanceReference, "2018-10-31", "Foo");

    }



    @Test
    public void insertEditor() throws IOException {
        Map<String, Object> fullyQualified = new LinkedHashMap<>();
        fullyQualified.put("http://test/foo", "foo");
        NexusInstanceReference instanceReference = TestObjectFactory.fooInstanceReference();
        instanceReference = instanceReference.toSubSpace(SubSpace.EDITOR);
        IndexingMessage message = TestObjectFactory.createIndexingMessage(instanceReference, fullyQualified);
        graphIndexing.insert(message);

        //cleanup
        graphIndexing.delete(instanceReference, "2018-10-31", "Foo");
    }


    @Test
    public void insertOriginalWithEditor() throws IOException {
        Map<String, Object> fullyQualified = new LinkedHashMap<>();
        fullyQualified.put("http://test/foo", "foo");
        Map<String, String> externalLink = new LinkedHashMap<>();
        externalLink.put(JsonLdConsts.ID, "https://schema.org/foo");
        fullyQualified.put("http://test/external", externalLink);

        Map<String, String> embedded = new LinkedHashMap<>();
        embedded.put("http://test/embedded/value", "embeddedfoo");
        Map<String, String> embedded2 = new LinkedHashMap<>();
        embedded2.put("http://test/embedded2/value", "embeddedfoo2");
        fullyQualified.put("http://test/embedded", Arrays.asList(embedded, embedded2));

        NexusInstanceReference instanceReference = TestObjectFactory.fooInstanceReference();
        IndexingMessage message = TestObjectFactory.createIndexingMessage(instanceReference, fullyQualified);
        graphIndexing.insert(message);

        NexusInstanceReference editorReference = TestObjectFactory.fooEditorInstanceReference();
        Map<String, Object> editorFullyQualified = new LinkedHashMap<>();
        editorFullyQualified.put("http://test/foo", "bar");
        Map<String, String> reference = new LinkedHashMap<>();
        reference.put(JsonLdConsts.ID, configuration.getAbsoluteUrl(instanceReference));
        editorFullyQualified.put(HBPVocabulary.INFERENCE_EXTENDS, reference);
        IndexingMessage editorMessage = TestObjectFactory.createIndexingMessage(editorReference, editorFullyQualified);
        graphIndexing.insert(editorMessage);


        //cleanup
        graphIndexing.delete(editorReference, "2018-10-31", "Foo");
        graphIndexing.delete(instanceReference, "2018-10-31", "Foo");
    }


    @Test
    public void updateOriginalWithEditor() throws IOException {

        insertOriginalWithEditor();

        Map<String, Object> fullyQualified = new LinkedHashMap<>();
        fullyQualified.put("http://test/foo", "fooNew");
        Map<String, String> externalLink = new LinkedHashMap<>();
        externalLink.put(JsonLdConsts.ID, "https://schema.org/foo");
        fullyQualified.put("http://test/external", externalLink);

        Map<String, String> embedded = new LinkedHashMap<>();
        embedded.put("http://test/embedded/value", "embeddedfoo");
        Map<String, String> embedded2 = new LinkedHashMap<>();
        embedded2.put("http://test/embedded2/value", "embeddedfoo2");
        fullyQualified.put("http://test/embedded", Arrays.asList(embedded, embedded2));

        NexusInstanceReference instanceReference = TestObjectFactory.fooInstanceReference();
        IndexingMessage message = TestObjectFactory.createIndexingMessage(instanceReference, fullyQualified);
        graphIndexing.update(message);

        //cleanup
        graphIndexing.delete(instanceReference, "2018-10-31", "Foo");
    }

}