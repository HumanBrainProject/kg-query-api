package org.humanbrainproject.knowledgegraph.control.arango;

import org.springframework.stereotype.Component;

@Component
public class ArangoReleasedDatabaseDriver extends ArangoDriver{
    public ArangoReleasedDatabaseDriver() {
        super("kg_released");
    }
}
