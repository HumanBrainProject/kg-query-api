package org.humanbrainproject.knowledgegraph.commons.propertyGraph.control;

public interface DatabaseConnection<DB> {

    DB getOrCreateDB();

    void clearData();
}
