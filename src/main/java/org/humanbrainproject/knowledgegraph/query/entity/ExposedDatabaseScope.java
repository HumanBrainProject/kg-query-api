package org.humanbrainproject.knowledgegraph.query.entity;

import org.humanbrainproject.knowledgegraph.annotations.NoTests;

@NoTests(NoTests.TRIVIAL)
public enum ExposedDatabaseScope {

    INFERRED, RELEASED;

    public DatabaseScope toDatabaseScope() {
        switch (this) {
            case RELEASED:
                return DatabaseScope.RELEASED;
            default:
                return DatabaseScope.INFERRED;
        }
    }

}
