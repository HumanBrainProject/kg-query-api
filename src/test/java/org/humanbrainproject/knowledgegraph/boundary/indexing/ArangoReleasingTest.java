package org.humanbrainproject.knowledgegraph.boundary.indexing;

import com.arangodb.ArangoCollection;
import com.arangodb.ArangoDatabase;
import org.humanbrainproject.knowledgegraph.TestDatabaseController;
import org.humanbrainproject.knowledgegraph.control.arango.ArangoNamingConvention;
import org.humanbrainproject.knowledgegraph.control.releasing.ReleasingController;
import org.humanbrainproject.knowledgegraph.entity.indexing.GraphIndexingSpec;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
@SpringBootTest
@Ignore("These tests require an arango execution environment. Make sure you have one properly configured first!")
public class ArangoReleasingTest {
    @Autowired
    GraphIndexing indexing;

    @Autowired
    TestDatabaseController testDatabaseController;

    @Autowired
    ReleasingController releasingController;

    @Autowired
    ArangoNamingConvention namingConvention;

    ArangoDatabase defaultDatabase;

    ArangoDatabase releasedDatabase;

    GraphIndexingSpec spec;
    GraphIndexingSpec spec2;
    GraphIndexingSpec nestedSpec;
    GraphIndexingSpec nestedReleaseSpec;

    @Before
    public void initDB() {
        indexing.databaseController = testDatabaseController;
        releasingController.controller = testDatabaseController;
        indexing.clearGraph();
        spec = new GraphIndexingSpec().setJsonOrJsonLdPayload("{\"foo\": \"bar\"}").setEntityName("foo/bar/barfoo/v0.0.1").setId("foo").setDefaultNamespace("http://test/");
        spec2 = new GraphIndexingSpec().setJsonOrJsonLdPayload("{\"bar\": \"foo\"}").setEntityName("foo/bar/barfoo/v0.0.1").setId("bar").setDefaultNamespace("http://test/");
        nestedSpec = new GraphIndexingSpec().setEntityName("foo/bar/barfoo/v0.0.1").setId("foobar").setJsonOrJsonLdPayload("{\"hello\": \"world\", \"nested\": {\"hi\": \"knowledgegraph\"}}").setDefaultNamespace("http://test/");
        defaultDatabase = testDatabaseController.getDefaultDB().getOrCreateDB();
        releasedDatabase = testDatabaseController.getReleasedDB().getOrCreateDB();
        nestedReleaseSpec = new GraphIndexingSpec().setJsonOrJsonLdPayload("{\"@type\": \"http://hbp.eu/minds#Release\", " +
                "\"http://hbp.eu/minds#releaseinstance\": {\"@id\": \"http://test/foo/bar/barfoo/v0.0.1/foobar\"}}").setEntityName("foo/prov/release/v0.0.1").setId("foo");
    }

    @Test
    public void releaseSingleInstance() {
        //given
        indexing.insertJsonOrJsonLd(spec);
        indexing.insertJsonOrJsonLd(spec2);

        //when
        String releaseJson = "{\"@type\": \"http://hbp.eu/minds#Release\", " +
                "\"http://hbp.eu/minds#releaseinstance\": {\"@id\": \"http://test/foo/bar/barfoo/v0.0.1/foo\"}}";

        GraphIndexingSpec releaseSpec = new GraphIndexingSpec().setJsonOrJsonLdPayload(releaseJson).setEntityName("foo/prov/release/v0.0.1").setId("foo");
        indexing.insertJsonOrJsonLd(releaseSpec);


        //then
        ArangoCollection collection = defaultDatabase.collection(namingConvention.getVertexLabel(spec.getEntityName()));
        assertTrue(collection.exists());
        assertTrue(collection.documentExists(spec.getId()));
        assertTrue(collection.documentExists(spec2.getId()));
        assertEquals(Long.valueOf(2), collection.count().getCount());

        ArangoCollection releasedCollection = releasedDatabase.collection(namingConvention.getVertexLabel(spec.getEntityName()));
        assertTrue(releasedCollection.documentExists(spec.getId()));
        assertEquals(Long.valueOf(1), releasedCollection.count().getCount());
    }


    @Test
    public void releaseNestedInstance() {
        //given
        indexing.insertJsonOrJsonLd(nestedSpec);

        //when
        indexing.insertJsonOrJsonLd(nestedReleaseSpec);

        //then
        ArangoCollection collection = defaultDatabase.collection(namingConvention.getVertexLabel(nestedSpec.getEntityName()));
        assertTrue(collection.exists());
        assertTrue(collection.documentExists(nestedSpec.getId()));
        assertEquals(Long.valueOf(1), collection.count().getCount());

        ArangoCollection releasedCollection = releasedDatabase.collection(namingConvention.getVertexLabel(nestedSpec.getEntityName()));
        assertTrue(releasedCollection.documentExists(nestedSpec.getId()));
        assertEquals(Long.valueOf(1), releasedDatabase.collection("rel-test-nested").count().getCount());
        assertTrue(releasedDatabase.collection("test-nested").documentExists("test-nested-foobar--1"));
        assertEquals(Long.valueOf(1), releasedCollection.count().getCount());
    }


    @Test
    public void releaseByLinkChange() {
        //given
        releaseNestedInstance();
        indexing.insertJsonOrJsonLd(spec);

        //when
        nestedReleaseSpec.setJsonOrJsonLdPayload("{\"@type\": \"http://hbp.eu/minds#Release\", " +
                "\"http://hbp.eu/minds#releaseinstance\": [{\"@id\": \"http://test/foo/bar/barfoo/v0.0.1/foobar\"}, {\"@id\": \"http://test/foo/bar/barfoo/v0.0.1/foo\"}]}");

        indexing.updateJsonOrJsonLd(nestedReleaseSpec);

        //then
        ArangoCollection collection = defaultDatabase.collection(namingConvention.getVertexLabel(spec.getEntityName()));
        assertTrue(collection.exists());
        assertTrue(collection.documentExists(spec.getId()));
        assertTrue(collection.documentExists(nestedSpec.getId()));
        assertEquals(Long.valueOf(2), collection.count().getCount());

        ArangoCollection releasedCollection = releasedDatabase.collection(namingConvention.getVertexLabel(spec.getEntityName()));
        assertTrue(releasedCollection.documentExists(spec.getId()));
        assertTrue(releasedCollection.documentExists(nestedSpec.getId()));
        assertEquals(Long.valueOf(1), releasedDatabase.collection("rel-test-nested").count().getCount());
        assertTrue(releasedDatabase.collection("test-nested").documentExists("test-nested-foobar--1"));
        assertEquals(Long.valueOf(2), releasedCollection.count().getCount());
    }

    @Test
    public void unreleaseInstanceByLinkChange() {
        //given
        releaseByLinkChange();

        //when
        String releaseJson = "{\"@type\": \"http://hbp.eu/minds#Release\", " +
                "\"http://hbp.eu/minds#releaseinstance\": [{\"@id\": \"http://test/foo/bar/barfoo/v0.0.1/foo\"}]}";
        GraphIndexingSpec releaseSpec = new GraphIndexingSpec().setJsonOrJsonLdPayload(releaseJson).setEntityName("foo/prov/release/v0.0.1").setId("foo");
        indexing.updateJsonOrJsonLd(releaseSpec);

        //then
        ArangoCollection collection = defaultDatabase.collection(namingConvention.getVertexLabel(nestedSpec.getEntityName()));
        assertTrue(collection.exists());
        assertTrue(collection.documentExists(spec.getId()));
        assertTrue(collection.documentExists(nestedSpec.getId()));
        assertEquals(Long.valueOf(2), collection.count().getCount());

        ArangoCollection releasedCollection = releasedDatabase.collection(namingConvention.getVertexLabel(nestedSpec.getEntityName()));
        assertTrue(releasedCollection.documentExists(spec.getId()));
        assertFalse(releasedCollection.documentExists(nestedSpec.getId()));
        assertEquals(Long.valueOf(0), (releasedDatabase.collection("rel-test-nested").count().getCount()));
        assertEquals(Long.valueOf(0), (releasedDatabase.collection("test-nested").count().getCount()));
        assertEquals(Long.valueOf(1), releasedCollection.count().getCount());
    }

    @Test
    public void unreleaseNestedInstanceByReleaseDeletion() {
        //given
        releaseNestedInstance();

        //when
        indexing.delete(nestedReleaseSpec);

        //then
        ArangoCollection collection = defaultDatabase.collection(namingConvention.getVertexLabel(nestedSpec.getEntityName()));
        assertTrue(collection.exists());
        assertTrue(collection.documentExists(nestedSpec.getId()));
        assertEquals(Long.valueOf(1), collection.count().getCount());

        ArangoCollection releasedCollection = releasedDatabase.collection(namingConvention.getVertexLabel(nestedSpec.getEntityName()));
        assertEquals(Long.valueOf(0), releasedCollection.count().getCount());
        assertEquals(Long.valueOf(0), releasedDatabase.collection("rel-test-nested").count().getCount());
        assertEquals(Long.valueOf(0), releasedDatabase.collection("test-nested").count().getCount());
    }

}