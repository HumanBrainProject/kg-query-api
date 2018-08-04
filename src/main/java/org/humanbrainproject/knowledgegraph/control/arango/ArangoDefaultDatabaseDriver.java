package org.humanbrainproject.knowledgegraph.control.arango;

import com.arangodb.ArangoDB;
import com.arangodb.ArangoDatabase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ArangoDefaultDatabaseDriver extends ArangoDriver{
    public ArangoDefaultDatabaseDriver() {
        super("kg");
    }
}
