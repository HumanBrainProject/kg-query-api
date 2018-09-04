package org.humanbrainproject.knowledgegraph.boundary.indexing;

import com.arangodb.ArangoCollection;
import com.arangodb.ArangoDatabase;
import com.arangodb.entity.CollectionType;
import org.humanbrainproject.knowledgegraph.TestDatabaseController;
import org.humanbrainproject.knowledgegraph.control.arango.ArangoNamingConvention;
import org.humanbrainproject.knowledgegraph.control.indexing.GraphSpecificationController;
import org.humanbrainproject.knowledgegraph.entity.indexing.GraphIndexingSpec;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Map;

import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
@SpringBootTest
@Ignore("These tests require an arango execution environment. Make sure you have one properly configured first!")
public class ArangoIndexingTest {
    @Autowired
    GraphIndexing indexing;

    @Autowired
    TestDatabaseController databaseController;

    @Autowired
    ArangoNamingConvention namingConvention;

    @Autowired
    GraphSpecificationController graphSpecificationController;

    GraphIndexingSpec spec;
    GraphIndexingSpec nestedSpec;

    ArangoDatabase defaultDb;

    private final static String DEFAULT_ENTITY_NAME = "foo/bar/barfoo/v0.0.1";

    @Before
    public void initDB(){
        indexing.databaseController = databaseController;
        indexing.clearGraph();
        spec = new GraphIndexingSpec().setEntityName("foo/bar/barfoo/v0.0.1").setId("foo").setJsonOrJsonLdPayload("{\"hello\": \"world\"}").setDefaultNamespace("http://test/");
        nestedSpec = new GraphIndexingSpec().setJsonOrJsonLdPayload("{\"hello\": \"world\", \"nested\": {\"hi\": \"knowledgegraph\"}}").setEntityName(DEFAULT_ENTITY_NAME).setId("foobar").setDefaultNamespace("http://test/");

        defaultDb = indexing.databaseController.getDefaultDB().getOrCreateDB();
    }

    @Test
    public void insert() {
        //given

        //when
        indexing.insertJsonOrJsonLd(spec);

        //then
        ArangoCollection collection = defaultDb.collection(namingConvention.getVertexLabel(spec.getEntityName()));
        assertTrue(collection.exists());
        assertTrue(collection.documentExists(spec.getId()));
        assertEquals(Long.valueOf(1), collection.count().getCount());
        assertEquals("world", collection.getDocument(spec.getId(), Map.class).get("http://test/hello"));
    }


    @Test
    public void insertNested(){
        //given

        //when
        indexing.insertJsonOrJsonLd(nestedSpec);

        //then
        ArangoCollection collection = defaultDb.collection(namingConvention.getVertexLabel(nestedSpec.getEntityName()));
        assertTrue(collection.exists());
        assertTrue(collection.documentExists(nestedSpec.getId()));

        ArangoCollection nestedCollection = defaultDb.collection("test-nested");
        assertTrue(nestedCollection.exists());
        assertTrue(nestedCollection.documentExists("test-nested-foobar--1"));
        assertEquals("knowledgegraph", nestedCollection.getDocument("test-nested-foobar--1", Map.class).get("http://test/hi"));

        ArangoCollection edgeCollection = defaultDb.collection("rel-test-nested");
        assertTrue(edgeCollection.exists());
        assertTrue(edgeCollection.getInfo().getType()== CollectionType.EDGES);
        assertEquals(Long.valueOf(1), edgeCollection.count().getCount());
    }


    @Test
    public void updateNestedWithRemovalOfNestedObject(){
        //given
        indexing.insertJsonOrJsonLd(nestedSpec);

        //when
        nestedSpec.setJsonOrJsonLdPayload("{\"hello\": \"world\"}");
        indexing.updateJsonOrJsonLd(nestedSpec);

        //then
        ArangoCollection collection = defaultDb.collection(namingConvention.getVertexLabel(nestedSpec.getEntityName()));
        assertTrue(collection.exists());
        assertTrue(collection.documentExists(nestedSpec.getId()));

        ArangoCollection nestedCollection = defaultDb.collection("test-nested");
        assertTrue(nestedCollection.exists());
        assertEquals(Long.valueOf(0), nestedCollection.count().getCount());

        ArangoCollection edgeCollection = defaultDb.collection("rel-test-nested");
        assertTrue(edgeCollection.exists());
        assertTrue(edgeCollection.getInfo().getType()== CollectionType.EDGES);
        assertEquals(Long.valueOf(0), edgeCollection.count().getCount());
    }


    @Test
    public void deleteNested() {
        //given
        insertNested();

        //when
        indexing.delete(nestedSpec);

        //then
        ArangoCollection collection = defaultDb.collection(namingConvention.getVertexLabel(DEFAULT_ENTITY_NAME));
        assertTrue("We don't delete once defined collections since they could be referenced by relations", collection.exists());
        assertEquals(Long.valueOf(0), collection.count().getCount());


        ArangoCollection nestedCollection = defaultDb.collection("test-nested");
        assertTrue("We don't delete once defined collections since they could be referenced by relations", nestedCollection.exists());
        assertEquals(Long.valueOf(0), nestedCollection.count().getCount());

        ArangoCollection edgeCollection = defaultDb.collection("rel-test-nested");
        assertTrue("We don't delete once defined collections since they could be referenced by relations", edgeCollection.exists());
        assertEquals(Long.valueOf(0), edgeCollection.count().getCount());
    }


    @Test
    public void update() {
        //given
        insert();
        spec.setJsonOrJsonLdPayload("{\"hello\": \"knowledgegraph\"}");

        //when
        indexing.updateJsonOrJsonLd(spec);

        //then
        ArangoCollection collection = defaultDb.collection(namingConvention.getVertexLabel(spec.getEntityName()));
        assertTrue(collection.exists());
        assertTrue(collection.documentExists(spec.getId()));
        assertEquals(Long.valueOf(1), collection.count().getCount());
        assertEquals("knowledgegraph", collection.getDocument(spec.getId(), Map.class).get("http://test/hello"));
    }

    @Test
    public void delete() {
        //given
        insert();
        insertNested();

        //when
        indexing.delete(spec);

        //then
        ArangoCollection collection = defaultDb.collection(namingConvention.getVertexLabel(spec.getEntityName()));
        assertTrue(collection.exists());
        assertFalse(collection.documentExists(spec.getId()));
        assertTrue(collection.documentExists(nestedSpec.getId()));
        assertEquals(Long.valueOf(1), collection.count().getCount());
    }


}