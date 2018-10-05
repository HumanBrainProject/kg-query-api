package org.humanbrainproject.knowledgegraph.control.json;

import org.humanbrainproject.knowledgegraph.control.arango.ArangoNamingConvention;

public class TestJsonTransformer extends JsonTransformer {

    public void setNamingConvention(ArangoNamingConvention namingConvention){
        this.namingConvention = namingConvention;
    }

}