package org.humanbrainproject.knowledgegraph.query.entity;

import java.util.List;

public class PythonClass {

    private final String name;
    private final List<PythonField> fields;


    public PythonClass(String name, List<PythonField> fields) {
        this.name = name;
        this.fields = fields;
    }

    public String getName() {
        return name;
    }

    public List<PythonField> getFields() {
        return fields;
    }
}
