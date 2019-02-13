package org.humanbrainproject.knowledgegraph.query.entity;

public class PythonField {

    private final String name;
    private final String type;
    private final String key;

    public PythonField(String name, String type, String key) {
        this.name = name;
        this.type = type;
        this.key = key;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public String getKey() {
        return key;
    }
}
