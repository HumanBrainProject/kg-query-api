package org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control;

import com.github.jsonldjava.core.JsonLdConsts;
import org.humanbrainproject.knowledgegraph.commons.jsonld.control.JsonLdStandardization;
import org.humanbrainproject.knowledgegraph.commons.jsonld.control.JsonTransformer;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoCollectionReference;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoDocumentReference;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.entity.Edge;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.entity.JsonPath;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.entity.Step;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.entity.Vertex;
import org.humanbrainproject.knowledgegraph.commons.vocabulary.ArangoVocabulary;
import org.humanbrainproject.knowledgegraph.commons.vocabulary.HBPVocabulary;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusInstanceReference;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusSchemaReference;
import org.humanbrainproject.knowledgegraph.testFactory.TestObjectFactory;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.*;

import static org.junit.Assert.*;

public class ArangoDocumentConverterTest {

    private ArangoDocumentConverter documentConverter;

    @Before
    public void setup() {
        this.documentConverter = new ArangoDocumentConverter();
        this.documentConverter.configuration = TestObjectFactory.createNexusConfiguration();
        this.documentConverter.standardization = Mockito.mock(JsonLdStandardization.class);
        Mockito.when(this.documentConverter.standardization.extendInternalReferencesWithRelativeUrl(Mockito.any(), Mockito.any())).thenCallRealMethod();
        this.documentConverter.jsonTransformer = new JsonTransformer();
    }


    @Test
    public void createJsonFromLinkingInstance() {
        ArangoDocumentReference reference = new ArangoDocumentReference(new ArangoCollectionReference("foo"), "bar");
        NexusInstanceReference from = new NexusInstanceReference(new NexusSchemaReference("foo", "core", "bar", "v1.0.0"), "fooFrom");
        NexusInstanceReference to = new NexusInstanceReference(new NexusSchemaReference("foo", "core", "bar", "v1.0.0"), "fooTo");
        NexusInstanceReference main = new NexusInstanceReference(new NexusSchemaReference("foo", "core", "bar", "v1.0.0"), "fooMain");

        Vertex vertex = new Vertex(TestObjectFactory.createQualifiedIndexingMessage(main, new HashMap<>()));

        String jsonFromLinkingInstance = documentConverter.createJsonFromLinkingInstance(reference, from, to, main, vertex);

        Map map = this.documentConverter.jsonTransformer.parseToMap(jsonFromLinkingInstance);

        assertEquals("http://foo/v0/data/foo/core/bar/v1.0.0/fooMain", map.get(JsonLdConsts.ID));
        assertEquals("foo/bar", map.get(ArangoVocabulary.ID));
        assertEquals("bar", map.get(ArangoVocabulary.KEY));
        assertEquals("foo/core/bar/v1.0.0/fooMain", map.get(HBPVocabulary.RELATIVE_URL_OF_INTERNAL_LINK));
        assertEquals("fooMain", map.get(ArangoVocabulary.NEXUS_UUID));
        assertEquals("foo/core/bar/v1.0.0/fooMain", map.get(ArangoVocabulary.NEXUS_RELATIVE_URL));
        assertEquals("foo/core/bar/v1.0.0/fooMain?rev=1", map.get(ArangoVocabulary.NEXUS_RELATIVE_URL_WITH_REV));
        assertEquals("foo", map.get(ArangoVocabulary.PERMISSION_GROUP));
        assertTrue(map.containsKey(ArangoVocabulary.INDEXED_IN_ARANGO_AT));
        assertEquals("foo-core-bar-v1_0_0/fooFrom", map.get(ArangoVocabulary.FROM));
        assertEquals("foo-core-bar-v1_0_0/fooTo", map.get(ArangoVocabulary.TO));
        assertEquals("foo/core/bar/v1.0.0", map.get(ArangoVocabulary.NAME));
    }


    @Test
    public void buildPath() {
        Step step = new Step("step1", 1);
        List<Step> remainingSteps = Arrays.asList(new Step("step2", 2), new Step("step3", 3));

        Map<String, Object> path = this.documentConverter.buildPath(step, remainingSteps);

        assertEquals(3, path.size());
        assertEquals(1, path.get("_orderNumber"));
        assertEquals("step1", path.get("_name"));

        Map step2 = (Map) path.get("_next");
        assertEquals(3, step2.size());
        assertEquals(2, step2.get("_orderNumber"));
        assertEquals("step2", step2.get("_name"));

        Map step3 = (Map) step2.get("_next");
        assertEquals(2, step3.size());
        assertEquals(3, step3.get("_orderNumber"));
        assertEquals("step3", step3.get("_name"));
    }


    @Test
    public void removePathFromMap() {
        Map<String, Object> map = new HashMap<>();

        Map<String, Object> foo = new HashMap<>();
        map.put("foo", "foo");

        Map<String, Object> bar = new HashMap<>();
        map.put("bar", "bar");

        documentConverter.removePathFromMap(map, new JsonPath("foo"));

        assertEquals(1, map.size());
        assertEquals("bar", map.get("bar"));
    }

    @Test
    public void removePathFromMapNested() {
        Map<String, Object> map = new HashMap<>();

        Map<String, Object> foo = new HashMap<>();
        HashMap<Object, Object> foo2 = new HashMap<>();
        map.put("foo", foo2);
        foo2.put("fooBar", "fooBar");

        Map<String, Object> bar = new HashMap<>();
        map.put("bar", "bar");

        documentConverter.removePathFromMap(map, new JsonPath(Arrays.asList(new Step("foo", 0), new Step("fooBar", 1))));

        assertEquals(1, map.size());
        assertEquals("bar", map.get("bar"));
    }

    @Test
    public void removePathFromMapMultiNested() {
        Map<String, Object> map = new HashMap<>();

        Map<String, Object> foo = new HashMap<>();
        HashMap<Object, Object> foo2 = new HashMap<>();
        map.put("foo", foo2);
        foo2.put("fooBar", "fooBar");
        foo2.put("barFoo", "barFoo");

        Map<String, Object> bar = new HashMap<>();
        map.put("bar", "bar");

        documentConverter.removePathFromMap(map, new JsonPath(Arrays.asList(new Step("foo", 0), new Step("fooBar", 1))));

        assertEquals(2, map.size());
        assertEquals(1, ((Map)map.get("foo")).size());
        assertEquals("barFoo", ((Map)map.get("foo")).get("barFoo"));
        assertEquals("bar", map.get("bar"));
    }


    @Test
    public void createJsonFromEdge() {
        //given
        ArangoDocumentReference reference = new ArangoDocumentReference(new ArangoCollectionReference("foo"), "bar");
        NexusInstanceReference source = new NexusInstanceReference(new NexusSchemaReference("foo", "core", "bar", "v1.0.0"), "fooSource");
        NexusInstanceReference target = new NexusInstanceReference(new NexusSchemaReference("foo", "core", "bar", "v1.0.0"), "fooTarget");

        HashMap<String, Object> qualifiedMapSource = new HashMap<>();
        qualifiedMapSource.put("http://foo/bar", "source");
        Vertex vertex = new Vertex(TestObjectFactory.createQualifiedIndexingMessage(source, qualifiedMapSource));
        Set<JsonPath> blacklist = new HashSet<>(Arrays.asList(new JsonPath("http://bar/foo")));

        HashMap<String, Object> qualifiedMapTarget = new HashMap<>();
        qualifiedMapTarget.put("http://foo/bar", "target");

        Edge edge = new Edge(new Vertex(TestObjectFactory.createQualifiedIndexingMessage(source, qualifiedMapTarget)), new JsonPath("http://reference"), target);

        //when
        String json = documentConverter.createJsonFromEdge(reference, vertex, edge, blacklist);

        //then
        Map map = this.documentConverter.jsonTransformer.parseToMap(json);
        assertEquals("foo/bar", map.get(ArangoVocabulary.ID));
        assertEquals("bar", map.get(ArangoVocabulary.KEY));
        assertTrue(map.containsKey(ArangoVocabulary.INDEXED_IN_ARANGO_AT));
        assertEquals("foo-core-bar-v1_0_0/fooSource", map.get(ArangoVocabulary.FROM));
        assertEquals("foo-core-bar-v1_0_0/fooTarget", map.get(ArangoVocabulary.TO));
        assertEquals("http://reference", map.get(ArangoVocabulary.NAME));


    }

    @Test
    public void createJsonFromVertex() {
        //given
        ArangoDocumentReference reference = new ArangoDocumentReference(new ArangoCollectionReference("foo"), "bar");
        NexusInstanceReference main = new NexusInstanceReference(new NexusSchemaReference("foo", "core", "bar", "v1.0.0"), "fooMain");

        HashMap<String, Object> qualifiedMap = new HashMap<>();
        qualifiedMap.put("http://foo/bar", "foobar");
        qualifiedMap.put("http://bar/foo", "barfoo");
        Vertex vertex = new Vertex(TestObjectFactory.createQualifiedIndexingMessage(main, qualifiedMap));

        Set<JsonPath> blacklist = new HashSet<>(Arrays.asList(new JsonPath("http://bar/foo")));

        //when
        String json = documentConverter.createJsonFromVertex(reference, vertex, blacklist);


        //then
        Map map = this.documentConverter.jsonTransformer.parseToMap(json);
        assertEquals("foobar", map.get("http://foo/bar"));
        assertEquals("http://foo/v0/data/foo/core/bar/v1.0.0/fooMain", map.get(JsonLdConsts.ID));
        assertEquals("foo/bar", map.get(ArangoVocabulary.ID));
        assertEquals("bar", map.get(ArangoVocabulary.KEY));
        assertEquals("foo/core/bar/v1.0.0/fooMain", map.get(HBPVocabulary.RELATIVE_URL_OF_INTERNAL_LINK));
        assertEquals("fooMain", map.get(ArangoVocabulary.NEXUS_UUID));
        assertEquals("foo/core/bar/v1.0.0/fooMain", map.get(ArangoVocabulary.NEXUS_RELATIVE_URL));
        assertEquals("foo/core/bar/v1.0.0/fooMain?rev=1", map.get(ArangoVocabulary.NEXUS_RELATIVE_URL_WITH_REV));
        assertEquals("foo", map.get(ArangoVocabulary.PERMISSION_GROUP));
        assertTrue(map.containsKey(ArangoVocabulary.INDEXED_IN_ARANGO_AT));
    }
}