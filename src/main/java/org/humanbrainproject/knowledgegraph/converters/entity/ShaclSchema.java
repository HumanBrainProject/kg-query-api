package org.humanbrainproject.knowledgegraph.converters.entity;

import com.github.jsonldjava.core.JsonLdConsts;
import org.humanbrainproject.knowledgegraph.query.entity.JsonDocument;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ShaclSchema {
    private static final String NEXUS_VOCAB = "https://bbp-nexus.epfl.ch/vocabs/nexus/core/terms/v0.1.0/";
    private static final String RDF_VOCAB = "http://www.w3.org/2000/01/rdf-schema#";

    private final JsonDocument shaclDocument;

    public ShaclSchema(JsonDocument shaclDocument) {
        this.shaclDocument = shaclDocument;
    }

    public List<ShaclShape> getShaclShapes() {
        Object shapes = null;
        if (this.shaclDocument.containsKey(JsonLdConsts.REVERSE)) {
            Object reverse = this.shaclDocument.get(JsonLdConsts.REVERSE);
            if (reverse instanceof Map && ((Map) reverse).containsKey(RDF_VOCAB + "isDefinedBy")) {
                shapes = ((Map) reverse).get(RDF_VOCAB + "isDefinedBy");
            }
        }

        List shapeList;
        if (shapes == null) {
            shapeList = Collections.emptyList();
        } else if (!(shapes instanceof List)) {
            shapeList = Collections.singletonList(shapes);
        } else {
            shapeList = (List) shapes;
        }
        return (List<ShaclShape>) (shapeList.stream().filter(s -> s instanceof Map).map(s -> new ShaclShape(new JsonDocument((Map) s))).collect(Collectors.toList()));

    }
}
