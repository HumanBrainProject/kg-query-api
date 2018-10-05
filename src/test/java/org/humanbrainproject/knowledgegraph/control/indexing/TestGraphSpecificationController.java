package org.humanbrainproject.knowledgegraph.control.indexing;

import org.humanbrainproject.knowledgegraph.control.json.JsonTransformer;
import org.humanbrainproject.knowledgegraph.control.jsonld.JsonLdStandardization;
import org.humanbrainproject.knowledgegraph.control.jsonld.JsonLdToVerticesAndEdges;

public class TestGraphSpecificationController extends GraphSpecificationController {

    public void setJsonLdStandardization(JsonLdStandardization jsonLdStandardization) {
        this.jsonLdStandardization = jsonLdStandardization;
    }

    public void setJsonLdToVerticesAndEdges(JsonLdToVerticesAndEdges jsonLdToVerticesAndEdges) {
        this.jsonLdToVerticesAndEdges = jsonLdToVerticesAndEdges;
    }

    public void setJsonTransformer(JsonTransformer jsonTransformer) {
        this.jsonTransformer = jsonTransformer;
    }
}