package org.humanbrainproject.knowledgegraph.commons.authorization.entity;

import java.util.Objects;

public class InternalMasterKey implements Credential {

    private final String id = "MASTER_KEY";

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        InternalMasterKey that = (InternalMasterKey) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
