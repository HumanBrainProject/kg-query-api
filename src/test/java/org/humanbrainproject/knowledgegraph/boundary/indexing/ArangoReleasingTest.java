package org.humanbrainproject.knowledgegraph.boundary.indexing;

import com.arangodb.ArangoCollection;
import com.arangodb.ArangoDatabase;
import org.humanbrainproject.knowledgegraph.control.arango.ArangoDriver;
import org.humanbrainproject.knowledgegraph.control.arango.ArangoNamingConvention;
import org.humanbrainproject.knowledgegraph.entity.jsonld.JsonLdVertex;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.configurationprocessor.json.JSONException;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
@SpringBootTest
@Ignore("These tests require an arango execution environment. Make sure you have one properly configured first!")
public class ArangoReleasingTest {
    @Autowired
    ArangoIndexing indexation;

    @Autowired
    @Qualifier("default-test")
    ArangoDriver defaultDb;

    @Autowired
    @Qualifier("released-test")
    ArangoDriver releasedDb;

    @Autowired
    ArangoNamingConvention namingConvention;

    ArangoDatabase defaultDatabase;

    ArangoDatabase releasedDatabase;

    JsonLdVertex v;
    JsonLdVertex v2;

    @Before
    public void initDB(){
        defaultDatabase = defaultDb.getOrCreateDB();
        releasedDatabase = releasedDb.getOrCreateDB();
        indexation.defaultDB = defaultDb;
        indexation.releasedDB = releasedDb;
        indexation.clearGraph();
        v = new JsonLdVertex().setEntityName("foo/bar/barfoo/v0.0.1").setKey("foo");
        v2 = new JsonLdVertex().setEntityName(v.getEntityName()).setKey("foo2");
    }

    @Test
    public void releaseSingleInstance() throws JSONException, IOException {
        //given
        indexation.transactionalJsonLdInsertion(Arrays.asList(v, v2));

        //when
        String releaseJson = "{\"@type\": \"http://hbp.eu/minds#Release\", " +
                "\"http://hbp.eu/minds#releaseinstance\": {\"@id\": \"http://test/foo/bar/barfoo/v0.0.1/foo\"}}";

        GraphIndexing.GraphIndexationSpec spec = new GraphIndexing.GraphIndexationSpec();
        spec.setJsonOrJsonLdPayload(releaseJson).setEntityName("foo/prov/release/v0.0.1").setId("foo");

        indexation.updateJsonOrJsonLd(spec);

        //then
        ArangoCollection collection = defaultDatabase.collection(namingConvention.getVertexLabel(v.getEntityName()));
        assertTrue(collection.exists());
        assertTrue(collection.documentExists("foo"));
        assertTrue(collection.documentExists("foo2"));
        assertEquals(Long.valueOf(2), collection.count().getCount());

        ArangoCollection releasedCollection = releasedDatabase.collection(namingConvention.getVertexLabel(v.getEntityName()));
        assertTrue(releasedCollection.documentExists("foo"));
        assertEquals(Long.valueOf(1), releasedCollection.count().getCount());
    }


    @Test
    public void releaseNestedInstance() throws JSONException, IOException {
        //given
        String nestedJson = "{\"hello\": \"world\", \"nested\": {\"hi\": \"knowledgegraph\"}}";
        GraphIndexing.GraphIndexationSpec spec = new GraphIndexing.GraphIndexationSpec();
        spec.setEntityName("foo/bar/barfoo/v0.0.1").setId("foobar").setJsonOrJsonLdPayload(nestedJson).setDefaultNamespace("http://test/");
        indexation.insertJsonOrJsonLd(spec);

        //when
        String releaseJson = "{\"@type\": \"http://hbp.eu/minds#Release\", " +
                "\"http://hbp.eu/minds#releaseinstance\": {\"@id\": \"http://test/foo/bar/barfoo/v0.0.1/foobar\"}}";
        spec.setJsonOrJsonLdPayload(releaseJson).setEntityName("foo/prov/release/v0.0.1").setId("foo");

        indexation.updateJsonOrJsonLd(spec);

        //then
        ArangoCollection collection = defaultDatabase.collection(namingConvention.getVertexLabel(v.getEntityName()));
        assertTrue(collection.exists());
        assertTrue(collection.documentExists("foobar"));
        assertEquals(Long.valueOf(1), collection.count().getCount());

        ArangoCollection releasedCollection = releasedDatabase.collection(namingConvention.getVertexLabel(v.getEntityName()));
        assertTrue(releasedCollection.documentExists("foobar"));
        assertEquals(Long.valueOf(1), releasedDatabase.collection("rel-test-nested").count().getCount());
        assertTrue(releasedDatabase.collection("test-nested").documentExists("test-nested-foobar--1"));
        assertEquals(Long.valueOf(1), releasedCollection.count().getCount());
    }


    @Test
    public void releaseByLinkChange() throws JSONException, IOException {
        //given
        releaseNestedInstance();
        indexation.transactionalJsonLdInsertion(Collections.singletonList(v));

        //when
        String releaseJson = "{\"@type\": \"http://hbp.eu/minds#Release\", " +
                "\"http://hbp.eu/minds#releaseinstance\": [{\"@id\": \"http://test/foo/bar/barfoo/v0.0.1/foobar\"}, {\"@id\": \"http://test/foo/bar/barfoo/v0.0.1/foo\"}]}";
        GraphIndexing.GraphIndexationSpec spec = new GraphIndexing.GraphIndexationSpec();
        spec.setJsonOrJsonLdPayload(releaseJson).setEntityName("foo/prov/release/v0.0.1").setId("foo");
        indexation.updateJsonOrJsonLd(spec);

        //then
        ArangoCollection collection = defaultDatabase.collection(namingConvention.getVertexLabel(v.getEntityName()));
        assertTrue(collection.exists());
        assertTrue(collection.documentExists("foobar"));
        assertTrue(collection.documentExists("foo"));
        assertEquals(Long.valueOf(2), collection.count().getCount());

        ArangoCollection releasedCollection = releasedDatabase.collection(namingConvention.getVertexLabel(v.getEntityName()));
        assertTrue(releasedCollection.documentExists("foobar"));
        assertTrue(releasedCollection.documentExists("foo"));
        assertEquals(Long.valueOf(1), releasedDatabase.collection("rel-test-nested").count().getCount());
        assertTrue(releasedDatabase.collection("test-nested").documentExists("test-nested-foobar--1"));
        assertEquals(Long.valueOf(2), releasedCollection.count().getCount());
    }

    @Test
    public void unreleaseInstanceByLinkChange() throws JSONException, IOException {
        //given
        releaseByLinkChange();

        //when
        String releaseJson = "{\"@type\": \"http://hbp.eu/minds#Release\", " +
                "\"http://hbp.eu/minds#releaseinstance\": [{\"@id\": \"http://test/foo/bar/barfoo/v0.0.1/foo\"}]}";
        GraphIndexing.GraphIndexationSpec spec = new GraphIndexing.GraphIndexationSpec();
        spec.setJsonOrJsonLdPayload(releaseJson).setEntityName("foo/prov/release/v0.0.1").setId("foo");
        indexation.updateJsonOrJsonLd(spec);

        //then
        ArangoCollection collection = defaultDatabase.collection(namingConvention.getVertexLabel(v.getEntityName()));
        assertTrue(collection.exists());
        assertTrue(collection.documentExists("foobar"));
        assertTrue(collection.documentExists("foo"));
        assertEquals(Long.valueOf(2), collection.count().getCount());

        ArangoCollection releasedCollection = releasedDatabase.collection(namingConvention.getVertexLabel(v.getEntityName()));
        assertFalse(releasedCollection.documentExists("foobar"));
        assertTrue(releasedCollection.documentExists("foo"));
        assertEquals(Long.valueOf(0), (releasedDatabase.collection("rel-test-nested").count().getCount()));
        assertEquals(Long.valueOf(0), (releasedDatabase.collection("test-nested").count().getCount()));
        assertEquals(Long.valueOf(1), releasedCollection.count().getCount());
    }


    @Test
    public void unreleaseNestedInstanceByDeletion() throws JSONException, IOException {
        //given
        releaseNestedInstance();

        //when
        indexation.delete("foo/prov/release/v0.0.1", "foo", null);

        //then
        ArangoCollection collection = defaultDatabase.collection(namingConvention.getVertexLabel(v.getEntityName()));
        assertTrue(collection.exists());
        assertTrue(collection.documentExists("foobar"));
        assertEquals(Long.valueOf(1), collection.count().getCount());

        ArangoCollection releasedCollection = releasedDatabase.collection(namingConvention.getVertexLabel(v.getEntityName()));
        assertEquals(Long.valueOf(0), releasedCollection.count().getCount());
        assertEquals(Long.valueOf(0), releasedDatabase.collection("rel-test-nested").count().getCount());
        assertEquals(Long.valueOf(0), releasedDatabase.collection("test-nested").count().getCount());
    }

}