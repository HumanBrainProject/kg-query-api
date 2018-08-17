package org.humanbrainproject.knowledgegraph.entity.specification;

import java.util.Collections;
import java.util.List;

public class Specification {
    public final String originalContext;
    public final String name;
    public final String rootSchema;
    public final List<SpecField> fields;
    public final String originalDocument;

    private String specificationId;

    public String getSpecificationId() {
        return specificationId;
    }

    public void setSpecificationId(String specificationId) {
        this.specificationId = specificationId;
    }

    public Specification(String originalContext, String name, String rootSchema, String originalDocument, List<SpecField> fields) {
        this.originalContext = originalContext;
        this.name = name;
        this.rootSchema = rootSchema;
        this.originalDocument = originalDocument;
        this.fields = fields==null ? Collections.emptyList() : Collections.unmodifiableList(fields);
    }
}
