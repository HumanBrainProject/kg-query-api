package org.humanbrainproject.knowledgegraph.query.entity.fieldFilter;

import org.humanbrainproject.knowledgegraph.annotations.NoTests;

@NoTests(NoTests.NO_LOGIC)
public enum Op {

    STARTS_WITH("starts with", ExampleValues.SIMPLE_STRING_EXAMPLE, false), ENDS_WITH("ends with", ExampleValues.SIMPLE_STRING_EXAMPLE, false), CONTAINS("contains", ExampleValues.SIMPLE_STRING_EXAMPLE, false), EQUALS("equals", ExampleValues.SIMPLE_STRING_EXAMPLE, false), REGEX("regular expression", ExampleValues.REGEX_EXAMPLE, false), MBB("minimal bounding box", ExampleValues.MBB_EXAMPLE, true);

    private final String name;
    private final String example;
    private boolean instanceFilter;

    Op(String name, String example, boolean instanceFilter){
          this.name = name;
          this.example = example;
          this.instanceFilter = instanceFilter;
    }

    public String getName() {
        return name;
    }

    public String getExample() {
        return example;
    }

    public boolean isInstanceFilter() {
        return instanceFilter;
    }
}