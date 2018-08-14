package org.humanbrainproject.knowledgegraph.boundary.indexation;

import com.arangodb.ArangoCollection;
import com.arangodb.ArangoDatabase;
import com.arangodb.entity.CollectionType;
import org.humanbrainproject.knowledgegraph.control.arango.ArangoDriver;
import org.humanbrainproject.knowledgegraph.control.arango.ArangoNamingConvention;
import org.humanbrainproject.knowledgegraph.entity.jsonld.JsonLdProperty;
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
import java.util.Map;

import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
@SpringBootTest
@Ignore("These tests require an arango execution environment. Make sure you have one properly configured first!")
public class ArangoIndexationTest {
    @Autowired
    ArangoIndexation indexation;

    @Autowired
    @Qualifier("default-test")
    ArangoDriver driver;

    @Autowired
    ArangoNamingConvention namingConvention;

    ArangoDatabase db;

    JsonLdVertex v;
    JsonLdVertex v2;
    private final static String DEFAULT_ENTITY_NAME = "foo/bar/barfoo/v0.0.1";

    @Before
    public void initDB(){
        db = driver.getOrCreateDB();
        indexation.defaultDB = driver;
        indexation.releasedDB = null;
        indexation.clearGraph();
        v = new JsonLdVertex().setEntityName("foo/bar/barfoo/v0.0.1").setKey("foo");
        v2 = new JsonLdVertex().setEntityName(v.getEntityName()).setKey("foo2");
    }

    @Test
    public void transactionalJsonLdInsertion() throws JSONException {
        //given

        //when
        indexation.transactionalJsonLdInsertion(Arrays.asList(v, v2));

        //then
        ArangoCollection collection = db.collection(namingConvention.getVertexLabel(v.getEntityName()));
        assertTrue(collection.exists());
        assertTrue(collection.documentExists("foo"));
        assertTrue(collection.documentExists("foo2"));
        assertEquals(Long.valueOf(2), collection.count().getCount());
    }


    @Test
    public void insertNestedJson() throws JSONException, IOException {
        //given
        String nestedJson = "{\"hello\": \"world\", \"nested\": {\"hi\": \"knowledgegraph\"}}";
        GraphIndexation.GraphIndexationSpec spec = new GraphIndexation.GraphIndexationSpec();
        spec.setEntityName(DEFAULT_ENTITY_NAME).setId("foobar").setJsonOrJsonLdPayload(nestedJson).setDefaultNamespace("http://test/");

        //when
        indexation.insertJsonOrJsonLd(spec);

        //then
        ArangoCollection collection = db.collection(namingConvention.getVertexLabel(v.getEntityName()));
        assertTrue(collection.exists());
        assertTrue(collection.documentExists("foobar"));

        ArangoCollection nestedCollection = db.collection("test-nested");
        assertTrue(nestedCollection.exists());
        assertTrue(nestedCollection.documentExists("test-nested-foobar--1"));
        assertEquals("knowledgegraph", nestedCollection.getDocument("test-nested-foobar--1", Map.class).get("http://test/hi"));

        ArangoCollection edgeCollection = db.collection("rel-test-nested");
        assertTrue(edgeCollection.exists());
        assertTrue(edgeCollection.getInfo().getType()== CollectionType.EDGES);
        assertEquals(Long.valueOf(1), edgeCollection.count().getCount());
    }


    @Test
    public void deleteNestedJson() throws JSONException, IOException {
        //given
        insertNestedJson();

        //when
        indexation.delete(DEFAULT_ENTITY_NAME, "foobar", null);

        //then
        ArangoCollection collection = db.collection(namingConvention.getVertexLabel(v.getEntityName()));
        assertTrue("We don't delete once defined collections since they could be referenced by relations", collection.exists());
        assertEquals(Long.valueOf(0), collection.count().getCount());


        ArangoCollection nestedCollection = db.collection("test-nested");
        assertTrue("We don't delete once defined collections since they could be referenced by relations", nestedCollection.exists());
        assertEquals(Long.valueOf(0), nestedCollection.count().getCount());

        ArangoCollection edgeCollection = db.collection("rel-test-nested");
        assertTrue("We don't delete once defined collections since they could be referenced by relations", edgeCollection.exists());
        assertEquals(Long.valueOf(0), edgeCollection.count().getCount());
    }


    @Test
    public void transactionalJsonLdUpdate() throws JSONException {
        //given
        transactionalJsonLdInsertion();
        v.addProperty(new JsonLdProperty().setName("hello").setValue("world"));

        //when
        indexation.transactionalJsonLdUpdate(Arrays.asList(v, v2));

        //then
        ArangoCollection collection = db.collection(namingConvention.getVertexLabel(v.getEntityName()));
        assertTrue(collection.exists());
        assertTrue(collection.documentExists("foo"));
        assertTrue(collection.documentExists("foo2"));
        assertEquals(Long.valueOf(2), collection.count().getCount());
        assertEquals("world", collection.getDocument("foo", Map.class).get("hello"));
    }

    @Test
    public void transctionalJsonLdDeletion() throws JSONException {
        //given
        transactionalJsonLdInsertion();

        //when
        indexation.transactionalJsonLdDeletion(v.getEntityName(), namingConvention.getKey(v), null);

        //then
        ArangoCollection collection = db.collection(namingConvention.getVertexLabel(v.getEntityName()));
        assertTrue(collection.exists());
        assertFalse(collection.documentExists("foo"));
        assertTrue(collection.documentExists("foo2"));
        assertEquals(Long.valueOf(1), collection.count().getCount());
    }


}