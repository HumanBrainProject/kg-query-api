package org.humanbrainproject.knowledgegraph.commons.propertyGraph.control;

import org.humanbrainproject.knowledgegraph.annotations.NoTests;

@NoTests(NoTests.NO_LOGIC)
public interface DatabaseConnection<DB> {

    DB getOrCreateDB();

    void clearData();
}
