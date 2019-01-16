package org.humanbrainproject.knowledgegraph.converters.entity;

import org.humanbrainproject.knowledgegraph.query.entity.JsonDocument;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ShaclSchema {
    private static final String NEXUS_VOCAB = "https://bbp-nexus.epfl.ch/vocabs/nexus/core/terms/v0.1.0/";

    private final JsonDocument shaclDocument;

    public ShaclSchema(JsonDocument shaclDocument){
        this.shaclDocument = shaclDocument;
    }

    public List<ShaclShape> getShaclShapes(){
        Object shapes = this.shaclDocument.get(NEXUS_VOCAB + "shapes");
        List shapeList;
        if(shapes ==null){
            shapeList = Collections.emptyList();
        }
        else if(!(shapes instanceof List)){
            shapeList = Collections.singletonList(shapes);
        }
        else{
            shapeList = (List)shapes;
        }
        return (List<ShaclShape>)(shapeList.stream().filter(s -> s instanceof Map).map(s -> new ShaclShape(new JsonDocument((Map) s))).collect(Collectors.toList()));

    }
}
