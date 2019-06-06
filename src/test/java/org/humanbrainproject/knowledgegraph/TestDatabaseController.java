package org.humanbrainproject.knowledgegraph;

import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.ArangoConnection;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.ArangoDatabaseFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class TestDatabaseController extends ArangoDatabaseFactory {

    @Autowired
    @Qualifier("released-test")
    ArangoConnection releasedTestDB;

    @Autowired
    @Qualifier("default-test")
    ArangoConnection defaultTestDB;

    @Override
    public ArangoConnection getReleasedDB() {
        return releasedTestDB;
    }

    @Override
    public ArangoConnection getDefaultDB(boolean asSystemUser) {
        return defaultTestDB;
    }
}
