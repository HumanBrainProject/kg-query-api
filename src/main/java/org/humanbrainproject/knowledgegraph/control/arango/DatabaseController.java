package org.humanbrainproject.knowledgegraph.control.arango;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Primary
@Component
public class DatabaseController {

    @Autowired
    @Qualifier("released")
    ArangoDriver releasedDB;

    @Autowired
    @Qualifier("default")
    ArangoDriver defaultDB;

    @Autowired
    ArangoRepository repository;


    public ArangoDriver getReleasedDB() {
        return releasedDB;
    }

    public ArangoDriver getDefaultDB() {
        return defaultDB;
    }

    public void clearGraph() {
        repository.clearDatabase(getDefaultDB().getOrCreateDB());
        if (releasedDB != null) {
            repository.clearDatabase(getReleasedDB().getOrCreateDB());
        }
    }

}
