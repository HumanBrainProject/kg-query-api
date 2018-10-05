package org.humanbrainproject.knowledgegraph.control.jsonld;

import org.humanbrainproject.knowledgegraph.control.json.JsonTransformer;

public class TestJsonLdStandardization extends JsonLdStandardization{

    public void setJsonTransformer(JsonTransformer jsonTransformer) {
        this.jsonTransformer = jsonTransformer;
    }

    public void setEndpoint(String endpoint){
        this.endpoint = endpoint;
    }

}