package org.humanbrainproject.knowledgegraph.control.arango;

import com.arangodb.ArangoCollection;
import com.arangodb.ArangoDatabase;
import org.humanbrainproject.knowledgegraph.TestDatabaseController;
import org.humanbrainproject.knowledgegraph.entity.jsonld.JsonLdProperty;
import org.humanbrainproject.knowledgegraph.entity.jsonld.JsonLdVertex;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.configurationprocessor.json.JSONException;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Map;

import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
@SpringBootTest
@Ignore("These tests require an arango execution environment. Make sure you have one properly configured first!")
public class ArangoRepositoryIntegrationTest {

    @Autowired
    ArangoRepository repository;

    @Autowired
    @Qualifier("default-test")
    ArangoDriver driver;

    @Autowired
    ArangoNamingConvention namingConvention;

    @Autowired
    TestDatabaseController testDatabaseController;

    ArangoDatabase db;

    @Before
    public void initDB(){
        db = testDatabaseController.getDefaultDB().getOrCreateDB();
        testDatabaseController.clearGraph();
    }

    @After
    public void clearDB(){
        testDatabaseController.clearGraph();
    }

    @Test
    public void testInsertVertex() throws JSONException {
        //given
        JsonLdVertex v = new JsonLdVertex().setEntityName("foo/bar/barfoo/v0.0.1").setKey("foo");

        //when
        repository.insertVertex(v, driver);

        //then
        assertTrue(db.collection(namingConvention.getVertexLabel(v.getEntityName())).documentExists(v.getKey()));
    }

    @Test
    public void testUpdateVertex() throws JSONException {
        //given
        JsonLdVertex v = new JsonLdVertex().setEntityName("foo/bar/barfoo/v0.0.1").setKey("foo");
        repository.insertVertex(v, driver);
        assertTrue(db.collection(namingConvention.getVertexLabel(v.getEntityName())).documentExists(v.getKey()));

        //when
        v.addProperty(new JsonLdProperty().setName("hello").setValue("world"));
        repository.updateVertex(v, driver);

        //then
        ArangoCollection collection = db.collection(namingConvention.getVertexLabel(v.getEntityName()));
        assertTrue(collection.documentExists(v.getKey()));
        assertEquals("world", collection.getDocument(v.getKey(), Map.class).get("hello"));
    }

    @Test
    public void testDeleteDocument() throws JSONException {
        //given
        JsonLdVertex v = new JsonLdVertex().setEntityName("foo/bar/barfoo/v0.0.1").setKey("foo");
        repository.insertVertex(v, driver);
        JsonLdVertex v2 = new JsonLdVertex().setEntityName("foo/bar/barfoo/v0.0.1").setKey("foo2");
        repository.insertVertex(v2, driver);
        String vertexLabel = namingConvention.getVertexLabel(v.getEntityName());

        //when
        repository.deleteDocument(namingConvention.getId(v), db);

        //then
        assertTrue(db.collection(vertexLabel).exists());
        assertFalse(db.collection(vertexLabel).documentExists("foo"));
        assertTrue(db.collection(vertexLabel).documentExists("foo2"));
    }

    @Test
    public void testDeleteLastDocument() throws JSONException {
        //given
        JsonLdVertex v = new JsonLdVertex().setEntityName("foo/bar/barfoo/v0.0.1").setKey("foo");
        repository.insertVertex(v, driver);

        //when
        repository.deleteDocument(namingConvention.getId(v), db);

        //then
        assertEquals(Long.valueOf(0), db.collection(namingConvention.getVertexLabel(v.getEntityName())).count().getCount());
    }



}