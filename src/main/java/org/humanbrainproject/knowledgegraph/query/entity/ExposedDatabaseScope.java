package org.humanbrainproject.knowledgegraph.query.entity;

import org.humanbrainproject.knowledgegraph.annotations.NoTests;

@NoTests(NoTests.TRIVIAL)
public enum ExposedDatabaseScope {

    RELEASED, INFERRED;

    public DatabaseScope toDatabaseScope() {
        switch (this) {
            case RELEASED:
                return DatabaseScope.RELEASED;
            default:
                return DatabaseScope.INFERRED;
        }
    }

}
