package org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control;

import org.humanbrainproject.knowledgegraph.annotations.NoTests;
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


    public ArangoConnection getReleasedDB() {
        return releasedDB;
    }

    public ArangoConnection getDefaultDB() {
        return defaultDB;
    }

    public ArangoConnection getInferredDB() {return inferredDB;}

    public ArangoConnection getInternalDB() {return arangoInternal;}


    public ArangoConnection getConnection(DatabaseScope scope){
        switch(scope) {
            case NATIVE:
                return getDefaultDB();
            case RELEASED:
                return getReleasedDB();
            case INFERRED:
                return getInferredDB();
        }
        return getDefaultDB();
    }

}
