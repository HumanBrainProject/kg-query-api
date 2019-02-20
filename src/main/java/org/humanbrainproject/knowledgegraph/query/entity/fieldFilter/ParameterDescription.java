package org.humanbrainproject.knowledgegraph.query.entity.fieldFilter;

import java.util.List;

public class ParameterDescription extends Exp {
    private final String parameterName;
    private final String operation;
    private final String exampleValue;
    private final List<String> path;

    public ParameterDescription(Parameter parameter, Op op, List<String> path) {
        this.parameterName = parameter.getName();
        this.operation = op.getName();
        this.exampleValue = op.getExample();
        this.path = path;
    }

    public String getParameterName() {
        return parameterName;
    }

    public String getOperation() {
        return operation;
    }

    public String getExampleValue() {
        return exampleValue;
    }

    public List<String> getPath() {
        return path;
    }
}
