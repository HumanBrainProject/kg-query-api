package org.humanbrainproject.knowledgegraph.converters.entity;

import com.github.jsonldjava.core.JsonLdConsts;
import org.humanbrainproject.knowledgegraph.query.entity.JsonDocument;

import java.util.Map;

public class ShaclProperty {
    private static final String NEXUS_VOCAB = "https://bbp-nexus.epfl.ch/vocabs/nexus/core/terms/v0.1.0/";
    private static final String RDF_VOCAB = "http://www.w3.org/2000/01/rdf-schema#";

    private static final String SHACL_VOCAB = "http://www.w3.org/ns/shacl#";

    private final JsonDocument property;

    public ShaclProperty(JsonDocument property){
        this.property = property;
    }


    public String getKey(){
        Object path = property.get(SHACL_VOCAB + "path");
        return path != null ? (String)((Map)path).get(JsonLdConsts.ID) : null;
    }

    public String getName(){
        return (String)property.get(SHACL_VOCAB+"name");
    }

    public boolean isLinkToInstance(){
        return property.containsKey(SHACL_VOCAB+"node");
    }






}
