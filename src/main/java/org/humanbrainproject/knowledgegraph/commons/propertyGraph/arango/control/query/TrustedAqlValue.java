package org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.query;

public class TrustedAqlValue {

    private final String value;

    public TrustedAqlValue(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
