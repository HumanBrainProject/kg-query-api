package org.humanbrainproject.knowledgegraph.suggestion.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.humanbrainproject.knowledgegraph.annotations.NoTests;

@NoTests(NoTests.NO_LOGIC)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Suggestion {
    private String id;
    private String label;
    private String type;


    public String getId() {
        return id;
    }

    public void setId(String id) {
        String pattern;
        pattern = ".+\\.humanbrainproject\\.(org|eu)\\/v\\d\\/data\\/";
        this.id = id.replaceFirst(pattern, "");
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
