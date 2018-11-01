package org.humanbrainproject.knowledgegraph.commons.jsonld.control;

import com.github.jsonldjava.utils.JsonUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;

public class JsonLdStandardizationTest {

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