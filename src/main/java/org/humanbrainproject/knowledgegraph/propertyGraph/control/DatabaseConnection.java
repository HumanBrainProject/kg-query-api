package org.humanbrainproject.knowledgegraph.propertyGraph.control;

public interface DatabaseConnection<DB> {

    DB getOrCreateDB();

    void clearData();
}
