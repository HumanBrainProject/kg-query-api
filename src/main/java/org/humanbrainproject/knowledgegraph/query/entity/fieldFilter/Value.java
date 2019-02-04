package org.humanbrainproject.knowledgegraph.query.entity.fieldFilter;

public class Value extends Exp {
    private String value;
    public Value(String v) {
        this.value = v;
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj != null && this.getClass() == obj.getClass()) {
            return this.value.equals(((Value)obj).value);
        }
        return false;
    }
}
