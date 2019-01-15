package org.humanbrainproject.knowledgegraph.commons.propertyGraph.entity;

import org.humanbrainproject.knowledgegraph.annotations.NoTests;

import java.util.Objects;

@NoTests(NoTests.TRIVIAL)
public class Step {
    private final String name;
    private final Integer orderNumber;

    public Step(String name, Integer orderNumber) {
        this.name = name;
        this.orderNumber = orderNumber;
    }

    public String getName() {
        return name;
    }

    public Integer getOrderNumber() {
        return orderNumber;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Step step = (Step) o;
        return Objects.equals(name, step.name) &&
                Objects.equals(orderNumber, step.orderNumber);
    }

    @Override
    public int hashCode() {

        return Objects.hash(name, orderNumber);
    }
}
