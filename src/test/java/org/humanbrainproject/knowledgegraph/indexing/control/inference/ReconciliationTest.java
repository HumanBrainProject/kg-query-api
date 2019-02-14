package org.humanbrainproject.knowledgegraph.indexing.control.inference;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.entity.Vertex;
import org.humanbrainproject.knowledgegraph.commons.vocabulary.HBPVocabulary;
import org.humanbrainproject.knowledgegraph.indexing.entity.Alternative;
import org.humanbrainproject.knowledgegraph.indexing.entity.IndexingMessage;
import org.humanbrainproject.knowledgegraph.indexing.entity.QualifiedIndexingMessage;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusInstanceReference;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusSchemaReference;
import org.humanbrainproject.knowledgegraph.query.entity.JsonDocument;
import org.humanbrainproject.knowledgegraph.testFactory.TestObjectFactory;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class ReconciliationTest {

    @Test
    public void testDateParsing() throws ParseException {
        LocalDateTime parse = LocalDateTime.parse("2018-11-08T07:15:11.289Z", DateTimeFormatter.ISO_ZONED_DATE_TIME);
        System.out.println(parse);
    }

    @Test
    public void testMergeVertices() {
        Reconciliation rec = new Reconciliation();
        JsonDocument doc = new JsonDocument();
        Set<Vertex> vertices = new HashSet<>();
        NexusInstanceReference iref = new NexusInstanceReference(new NexusSchemaReference("org", "dom", "schema", "v1"), "123");
        NexusInstanceReference iref2 = new NexusInstanceReference(new NexusSchemaReference("org", "dom", "schema", "v1"), "456");
        NexusInstanceReference iref3 = new NexusInstanceReference(new NexusSchemaReference("org", "dom", "schema", "v1"), "789");


        Map<String, Object> m1 = new HashMap<>();
        m1.put("name", "test 1");
        m1.put("desc", "desc 1");
        m1.put("activity", "activity");
        m1.put(HBPVocabulary.PROVENANCE_MODIFIED_AT, "2018-11-10T07:15:11.289Z");
        m1.put(HBPVocabulary.PROVENANCE_LAST_MODIFICATION_USER_ID, "123");

        QualifiedIndexingMessage message1 = TestObjectFactory.createQualifiedIndexingMessage(iref, m1);
        Vertex vertex1 = new Vertex(message1);

        Map<String, Object> m2 = new HashMap<>();
        m2.put("name", "test 2");
        m2.put("desc", "another desc");
        m2.put("activity", "activity");
        m2.put(HBPVocabulary.PROVENANCE_MODIFIED_AT, "2018-11-08T07:15:11.289Z");
        m2.put(HBPVocabulary.PROVENANCE_LAST_MODIFICATION_USER_ID, "456");

        QualifiedIndexingMessage message2 = TestObjectFactory.createQualifiedIndexingMessage(iref2, m2);
        Vertex vertex2 = new Vertex(message2);

        Map<String, Object> m3 = new HashMap<>();
        m3.put("name", "test 1");
        m3.put("desc", "no desc");
        m3.put(HBPVocabulary.PROVENANCE_MODIFIED_AT, "2018-12-08T07:15:11.289Z");
        m3.put(HBPVocabulary.PROVENANCE_LAST_MODIFICATION_USER_ID, "789");

        QualifiedIndexingMessage message3 = TestObjectFactory.createQualifiedIndexingMessage(iref3, m3);
        Vertex vertex3 = new Vertex(message3);

        vertices.add(vertex1);
        vertices.add(vertex2);
        vertices.add(vertex3);

        rec.mergeVertex(doc, vertices);
        Assert.assertNotEquals(doc, null);
        Assert.assertEquals( "test 1", doc.get("name"));
        Assert.assertEquals( "no desc" , doc.get("desc"));
        Assert.assertEquals("activity", doc.get("activity"));
        Map alternatives = (HashMap) doc.get(HBPVocabulary.INFERENCE_ALTERNATIVES);
        List<Alternative> altNames = (List<Alternative>) alternatives.get("name");
        List<Alternative> alts = (List<Alternative>) alternatives.get("desc");
        Assert.assertEquals(2, alts.size());
        Assert.assertEquals(1, altNames.size());

    }


}