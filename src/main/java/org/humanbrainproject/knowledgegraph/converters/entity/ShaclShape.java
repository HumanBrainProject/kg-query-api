package org.humanbrainproject.knowledgegraph.converters.entity;

import com.github.jsonldjava.core.JsonLdConsts;
import org.humanbrainproject.knowledgegraph.query.entity.JsonDocument;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ShaclShape {
    private static final String NEXUS_VOCAB = "https://bbp-nexus.epfl.ch/vocabs/nexus/core/terms/v0.1.0/";
    private static final String RDF_VOCAB = "http://www.w3.org/2000/01/rdf-schema#";
    private static final String SHACL_VOCAB = "http://www.w3.org/ns/shacl#";

    private final JsonDocument shape;

    public ShaclShape(JsonDocument shape){
        this.shape = shape;
    }

    public String getLabel(){
        return (String) ((Map)shape.get(RDF_VOCAB+"label")).get(JsonLdConsts.VALUE);
    }

    public List<ShaclProperty> getProperties(){
        List<Map> properties = lookupProperties();
        return properties.stream().map(p -> new ShaclProperty(new JsonDocument(p))).collect(Collectors.toList());
    }

    private List<Map> lookupProperties(){
        String propertyKey = SHACL_VOCAB + "property";
        List<Map> result;
        if(this.shape.containsKey(propertyKey)){
            Object o = this.shape.get(propertyKey);
            if(o instanceof List){
                result = ((List<Map>)o);
            }
            else{
                result = Collections.singletonList((Map)o);
            }
        }
        else{
            result = new ArrayList<>();
            for (Object value : this.shape.values()) {
                if(value instanceof Map && ((Map)value).containsKey(JsonLdConsts.LIST)){
                    List l = (List)((Map)value).get(JsonLdConsts.LIST);
                    for (Object v : l) {
                        if(v instanceof Map && ((Map)v).containsKey(propertyKey)) {
                            Object propertyValue = ((Map) v).get(propertyKey);
                            if(propertyValue instanceof List){
                                result.addAll((List)propertyValue);
                            }
                            else if(propertyValue instanceof Map){
                                result.add((Map)propertyValue);
                            }
                        }
                    }
                }
            }
        }
        return result.stream().map(e -> {e.put("shapeDeclaration", this.shape.get(JsonLdConsts.ID)); return e;}).collect(Collectors.toList());

    }





}
