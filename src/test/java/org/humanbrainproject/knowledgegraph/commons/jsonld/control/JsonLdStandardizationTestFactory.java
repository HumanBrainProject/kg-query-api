package org.humanbrainproject.knowledgegraph.commons.jsonld.control;

public class JsonLdStandardizationTestFactory {

    public static JsonLdStandardization createJsonLdStandardization(){
        JsonLdStandardization standardization = new JsonLdStandardization();
        standardization.endpoint = null;
        standardization.jsonTransformer = new JsonTransformer();
        return standardization;
    }
}