package org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control;

import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.aql.AQL;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.aql.TrustedAqlValue;

public class EqualsFilter {

    public final TrustedAqlValue key;
    public final String value;

    public EqualsFilter(TrustedAqlValue key, String value) {
        this.key = key;
        this.value = value;
    }

    public EqualsFilter(String key, String value) {
        this.key = AQL.preventAqlInjection(key);
        this.value = value;
    }

    public TrustedAqlValue getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }
}
