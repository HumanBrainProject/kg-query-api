package org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control;

import org.humanbrainproject.knowledgegraph.annotations.NoTests;
import org.humanbrainproject.knowledgegraph.commons.authorization.control.AuthorizationContext;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.exceptions.IllegalDatabaseScope;
import org.humanbrainproject.knowledgegraph.query.entity.DatabaseScope;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Primary
@Component
@NoTests(NoTests.TRIVIAL)
public class ArangoDatabaseFactory {

    @Autowired
    @Qualifier("released")
    ArangoConnection releasedDB;

    @Autowired
    @Qualifier("default")
    ArangoConnection defaultDB;

    @Autowired
    @Qualifier("inferred")
    ArangoConnection inferredDB;

    @Autowired
    @Qualifier("internal")
    ArangoConnection arangoInternal;

    @Autowired
    AuthorizationContext authorizationContext;

    public ArangoConnection getReleasedDB() {
        return releasedDB;
    }

    public ArangoConnection getDefaultDB(boolean asSystemUser) {
        if(asSystemUser || authorizationContext.isAllowedToSeeCuratedInstances()){
            return defaultDB;
        }
        throw new IllegalDatabaseScope("You've tried to read from the default space - this is not allowed with your token.");
    }

    public ArangoConnection getInferredDB(boolean asSystemUser) {
        if(asSystemUser || authorizationContext.isAllowedToSeeCuratedInstances()){
            return inferredDB;
        }
        throw new IllegalDatabaseScope("You've tried to read from the curated space - this is not allowed with your token.");
    }

    public ArangoConnection getInternalDB() {
        return arangoInternal;
    }


    public ArangoConnection getConnection(DatabaseScope scope) {
        switch (scope) {
            case NATIVE:
                return getDefaultDB(false);
            case RELEASED:
                return getReleasedDB();
            case INFERRED:
                return getInferredDB(false);
        }
        return getDefaultDB(false);
    }

}
