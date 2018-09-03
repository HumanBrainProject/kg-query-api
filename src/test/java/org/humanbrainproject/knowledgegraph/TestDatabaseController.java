package org.humanbrainproject.knowledgegraph;

import org.humanbrainproject.knowledgegraph.control.arango.ArangoDriver;
import org.humanbrainproject.knowledgegraph.control.arango.DatabaseController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class TestDatabaseController extends DatabaseController{

    @Autowired
    @Qualifier("released-test")
    ArangoDriver releasedTestDB;

    @Autowired
    @Qualifier("default-test")
    ArangoDriver defaultTestDB;

    @Override
    public ArangoDriver getReleasedDB() {
        return releasedTestDB;
    }

    @Override
    public ArangoDriver getDefaultDB() {
        return defaultTestDB;
    }
}
