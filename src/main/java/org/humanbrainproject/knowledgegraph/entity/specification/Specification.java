package org.humanbrainproject.knowledgegraph.entity.specification;

import java.util.Collections;
import java.util.List;

public class Specification {
    public final String originalContext;
    public final String name;
    public final String rootSchema;
    public final List<SpecField> fields;

    public Specification(String originalContext, String name, String rootSchema, List<SpecField> fields) {
        this.originalContext = originalContext;
        this.name = name;
        this.rootSchema = rootSchema;
        this.fields = fields==null ? Collections.emptyList() : Collections.unmodifiableList(fields);
    }
}
