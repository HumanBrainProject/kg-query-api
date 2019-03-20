package org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.aql;

import org.humanbrainproject.knowledgegraph.annotations.NoTests;

@NoTests(NoTests.NO_LOGIC)
public class TrustedAqlValue {


    private final String value;

    public TrustedAqlValue(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
