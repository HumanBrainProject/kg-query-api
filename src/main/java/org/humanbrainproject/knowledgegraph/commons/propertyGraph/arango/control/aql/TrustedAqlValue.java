package org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.aql;

import org.humanbrainproject.knowledgegraph.annotations.NoTests;

@NoTests(NoTests.NO_LOGIC)
public class TrustedAqlValue {

    /**
     * With applying this method to a string, you state that you have checked, the provided string does not contain any unchecked "dynamic" part (such as user inputs).
     *
     * The string should consist only of string constants, other trusted values and concatenations out of them. Dynamic parts can be defined with property placeholders - such as ${foo} since they are checked before insertion for their trustworthiness.
     *
     * Please be careful when executing your checks! Passing non-validated strings to this method can introduce vulnerabilities to the system!!!
     */
    public final static TrustedAqlValue trust(String trustedString){
        return new TrustedAqlValue(trustedString);
    }

    private final String value;

    public TrustedAqlValue(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
