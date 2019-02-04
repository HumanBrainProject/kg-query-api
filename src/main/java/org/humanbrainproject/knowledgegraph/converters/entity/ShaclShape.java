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
        Map label = ((Map)shape.get(RDF_VOCAB+"label"));
        return label==null ? (String)getTargetClass() : (String)label.get(JsonLdConsts.VALUE);
    }

    public Object getTargetClass(){
        Object targetClass = this.shape.get(SHACL_VOCAB + "targetClass");
        return targetClass instanceof Map ? ((Map)targetClass).get(JsonLdConsts.ID) : null;
    }

    public boolean isTargeted(){
        return getTargetClass()!=null;
    }

    public String getId(){
            return (String) (shape.get(JsonLdConsts.ID));
    }


    public List<String> getNodes(){
        List<Map> nodes = new ArrayList<>();
        Object directNode = shape.get(SHACL_VOCAB + "node");
        if(directNode instanceof Map){
            nodes.add((Map)directNode);
        }
        Object and = shape.get(SHACL_VOCAB + "and");
        if(and instanceof Map){
            Object list = ((Map) and).get(JsonLdConsts.LIST);
            if(list instanceof List) {
                for (Object a : ((List) list)) {
                    if (a instanceof Map && ((Map)a).get(SHACL_VOCAB+"node")instanceof Map) {
                        nodes.add((Map)((Map) a).get(SHACL_VOCAB+"node"));
                    }
                }
            }
        }
        return nodes.stream().map(m -> (String)m.get(JsonLdConsts.ID)).collect(Collectors.toList());
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
