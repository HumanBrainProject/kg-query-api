package org.humanbrainproject.knowledgegraph.query.entity;

import org.humanbrainproject.knowledgegraph.annotations.NoTests;

import java.util.Collections;
import java.util.List;

@NoTests(NoTests.TRIVIAL)
public class Specification {
    public final String originalContext;
    public final String name;
    public final String rootSchema;
    public final List<SpecField> fields;
    public final JsonDocument originalDocument;

    private String specificationId;

    public String getSpecificationId() {
        return specificationId;
    }

    public void setSpecificationId(String specificationId) {
        this.specificationId = specificationId;
    }

    public Specification(String originalContext, String name, String rootSchema, JsonDocument originalDocument, List<SpecField> fields) {
        this.originalContext = originalContext;
        this.name = name;
        this.rootSchema = rootSchema;
        this.originalDocument = originalDocument;
        this.fields = fields==null ? Collections.emptyList() : Collections.unmodifiableList(fields);
    }
}
