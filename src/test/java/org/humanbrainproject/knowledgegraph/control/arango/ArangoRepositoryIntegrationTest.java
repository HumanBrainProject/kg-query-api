package org.humanbrainproject.knowledgegraph.control.arango;

import com.arangodb.ArangoCollection;
import com.arangodb.ArangoDatabase;
import org.humanbrainproject.knowledgegraph.entity.jsonld.JsonLdEdge;
import org.humanbrainproject.knowledgegraph.entity.jsonld.JsonLdProperty;
import org.humanbrainproject.knowledgegraph.entity.jsonld.JsonLdVertex;
import org.junit.*;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.configurationprocessor.json.JSONException;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Arrays;
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

    ArangoDatabase db;

    @Before
    public void initDB(){
        db = driver.getOrCreateDB();
        repository.clearDatabase(db);
    }

    @After
    public void clearDB(){
        repository.clearDatabase(db);
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


    @Test
    public void testGetEdgesToBeRemoved() throws JSONException {
        //given
        //Setup a structure with two vertices and one edge connecting them.
        JsonLdProperty p = new JsonLdProperty().setName("hello").setValue("world");
        JsonLdEdge edge = new JsonLdEdge().setReference("http://test/bar/foo/foobar/v0.0.1/bar").setName("bla");
        JsonLdVertex v = new JsonLdVertex().setEntityName("foo/bar/barfoo/v0.0.1").setKey("foo").addProperty(p).addEdge(edge);
        JsonLdVertex v2 = new JsonLdVertex().setEntityName("bar/foo/foobar/v0.0.1").setKey("bar");
        repository.uploadToPropertyGraph(Arrays.asList(v, v2), driver);

        assertTrue(db.collection(namingConvention.getVertexLabel(v.getEntityName())).documentExists(v.getKey()));
        assertTrue(db.collection(namingConvention.getVertexLabel(v2.getEntityName())).documentExists(v2.getKey()));
        assertTrue(db.collection(namingConvention.getEdgeLabel(edge.getName())).documentExists(namingConvention.getReferenceKey(v, edge)));


        //when
        //Replace the previous edge with another (unresolved) edge
        JsonLdEdge edgeNew = new JsonLdEdge().setReference("http://test/bar/foo/foobar/v0.0.1/bar2").setName("blabla");
        v.getEdges().clear();
        v.addEdge(edgeNew);
        repository.uploadToPropertyGraph(Arrays.asList(v, v2), driver);

        //then
        assertTrue(db.collection(namingConvention.getVertexLabel(v.getEntityName())).documentExists(v.getKey()));
        assertTrue(db.collection(namingConvention.getVertexLabel(v2.getEntityName())).documentExists(v2.getKey()));
        assertTrue(db.collection(namingConvention.getEdgeLabel(edgeNew.getName())).documentExists(namingConvention.getReferenceKey(v, edgeNew)));
        assertEquals(Long.valueOf(0), db.collection(namingConvention.getEdgeLabel(edge.getName())).count().getCount());

    }


}