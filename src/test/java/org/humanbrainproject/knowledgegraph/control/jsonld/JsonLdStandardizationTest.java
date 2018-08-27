package org.humanbrainproject.knowledgegraph.control.jsonld;

import org.apache.commons.io.IOUtils;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;

public class JsonLdStandardizationTest {

    @Test
    @Ignore("Integration test - needs an available jsonld service")
    public void fullyQualify() throws IOException {
        String json = IOUtils.toString(this.getClass().getResourceAsStream("/recursive_context.json"), "UTF-8");
        System.out.println(json);
        JsonLdStandardization standardization = new JsonLdStandardization();
        standardization.endpoint = "http://localhost:3000";
        Object qualified = standardization.fullyQualify(json);
        System.out.println(qualified);
    }
}