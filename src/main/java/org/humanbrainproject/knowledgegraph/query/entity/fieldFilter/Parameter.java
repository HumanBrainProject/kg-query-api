package org.humanbrainproject.knowledgegraph.query.entity.fieldFilter;

public class Parameter extends Exp {
    private String name;
    public Parameter(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj != null && this.getClass() == obj.getClass()) {
            return this.name.equals(((Parameter)obj).name);
        }
        return false;
    }
}
