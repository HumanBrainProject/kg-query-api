package org.humanbrainproject.knowledgegraph.query.entity;

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
