package org.humanbrainproject.knowledgegraph.commons.jsonld.control;

import com.github.jsonldjava.utils.JsonUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.util.Map;

public class JsonLdStandardizationTest {

    JsonTransformer json = new JsonTransformer();

    @Test
    @Ignore("This test requires the running service to be executable.")
    public void fullyQualify() throws IOException {
        String json = IOUtils.toString(this.getClass().getResourceAsStream("/recursive_context.json"), "UTF-8");
        System.out.println(json);
        JsonLdStandardization standardization = new JsonLdStandardization();
        standardization.jsonTransformer = new JsonTransformer();
        standardization.endpoint = "http://localhost:3000";
        Object qualified = standardization.fullyQualify(json);
        System.out.println(qualified);
    }


    @Test
    public void qualify() {
        String source = "{'@context': { 'test': 'http://test/'}, 'test:foo': 'bar', 'test:bar': 1, 'test:foobar': ['hello'], 'test:barfoo': ['hello', 'world']}";
        JsonLdStandardization standardization = new JsonLdStandardization();
        standardization.jsonTransformer = json;
        Map qualified = standardization.fullyQualify(source);
        Assert.assertEquals(json.normalize("{'http://test/bar': 1, 'http://test/barfoo': ['hello', 'world'], 'http://test/foo': 'bar', 'http://test/foobar': 'hello'}"), json.getMapAsJson(qualified));
    }


    @Test
    public void flattenList() {
        String source = "{'foo': {'@list': ['bar', 'foo', 'foobar']}}";
        Map map = json.parseToMap(source);
        new JsonLdStandardization().flattenLists(map, null, null);
        Assert.assertEquals(json.normalize("{'foo': ['bar', 'foo', 'foobar']}"), json.getMapAsJson(map));
    }


    @Test
    public void flattenEmptyList() {
        String source = "{'foo': {'@list': []}}";
        Map map = json.parseToMap(source);
        new JsonLdStandardization().flattenLists(map, null, null);
        Assert.assertEquals(json.normalize("{'foo': []}"), json.getMapAsJson(map));
    }

    @Test
    public void flattenNullList() {
        String source = "{'foo': {'@list': null}}";
        Map map = json.parseToMap(source);
        new JsonLdStandardization().flattenLists(map, null, null);
        Assert.assertEquals(json.normalize("{'foo': null}"), json.getMapAsJson(map));
    }


    @Test
    public void filterKeysByVocabBlacklists() throws IOException {
        String json = "{\n" +
                "  \"https://bbp-nexus.epfl.ch/vocabs/nexus/core/terms/v0.1.0/self\": {\n" +
                "    \"@id\": \"https://nexus-dev.humanbrainproject.org/v0/data/neuralactivity/experiment/patchedcell/v0.1.0/49ce2d7b-0527-46c3-83ac-d443918394b7\"\n" +
                "  },\n" +
                "  \"http://schema.hbp.eu/internal#rev\": 2,\n" +
                "  \"http://schema.hbp.eu/internal#embedded\": true,\n" +
                "  \"@id\": \"http://schema.hbp.eu/neuralactivity/experiment/patchedcell/v0.1.0#links#49ce2d7b-0527-46c3-83ac-d443918394b7--1\",\n" +
                "  \"https://bbp-nexus.epfl.ch/vocabs/nexus/core/terms/v0.1.0/outgoing\": {\n" +
                "    \"@id\": \"https://nexus-dev.humanbrainproject.org/v0/data/neuralactivity/experiment/patchedcell/v0.1.0/49ce2d7b-0527-46c3-83ac-d443918394b7/outgoing\"\n" +
                "  },\n" +
                "  \"https://bbp-nexus.epfl.ch/vocabs/nexus/core/terms/v0.1.0/schema\": {\n" +
                "    \"@id\": \"https://nexus-dev.humanbrainproject.org/v0/schemas/neuralactivity/experiment/patchedcell/v0.1.0\"\n" +
                "  },\n" +
                "  \"_permissionGroup\": \"neuralactivity\",\n" +
                "  \"https://bbp-nexus.epfl.ch/vocabs/nexus/core/terms/v0.1.0/incoming\": {\n" +
                "    \"@id\": \"https://nexus-dev.humanbrainproject.org/v0/data/neuralactivity/experiment/patchedcell/v0.1.0/49ce2d7b-0527-46c3-83ac-d443918394b7/incoming\"\n" +
                "  }\n" +
                "}";
        Object o = JsonUtils.fromString(json);
        JsonLdStandardization standardization = new JsonLdStandardization();
        Object result = standardization.filterKeysByVocabBlacklists(o);
        String resultString = JsonUtils.toString(result);
        System.out.println(resultString);


    }
}