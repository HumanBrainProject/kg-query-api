package org.humanbrainproject.knowledgegraph.query.entity.fieldFilter;

public class FieldFilter {
    private Op op;
    private Value value;
    private Parameter parameter;
    public FieldFilter(Op op, Value value, Parameter parameter) {
        this.op = op;
        this.value = value;
        this.parameter = parameter;
    }

    public Op getOp() {
        return op;
    }

    public Value getValue() {
        return value;
    }

    public Parameter getParameter() {
        return parameter;
    }

}
