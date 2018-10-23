package org.humanbrainproject.knowledgegraph.propertyGraph.arango.control;

import org.humanbrainproject.knowledgegraph.query.entity.DatabaseScope;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Primary
@Component
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
    ArangoRepository repository;


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
        return null;
    }

    public void clearGraph() {
        repository.clearDatabase(getDefaultDB().getOrCreateDB());
        if (releasedDB != null) {
            repository.clearDatabase(getReleasedDB().getOrCreateDB());
        }
        if(inferredDB !=null){
            repository.clearDatabase(getInferredDB().getOrCreateDB());
        }
    }

}
