package org.humanbrainproject.knowledgegraph.commons.propertyGraph.entity;

import org.humanbrainproject.knowledgegraph.annotations.NoTests;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

@NoTests(NoTests.TRIVIAL)
public class JsonPath extends ArrayList<Step> {

    public JsonPath(int i) {
        super(i);
    }

    public JsonPath() {
    }

    public JsonPath(Collection<? extends Step> collection) {
        super(collection);
    }

    public JsonPath(Step singleInstance){
        this(Collections.singleton(singleInstance));
    }

    public JsonPath(String singleFieldName){
        this(new Step(singleFieldName,null));
    }
}
