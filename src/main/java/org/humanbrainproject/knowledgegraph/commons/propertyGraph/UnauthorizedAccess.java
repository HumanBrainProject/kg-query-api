package org.humanbrainproject.knowledgegraph.commons.propertyGraph;

import org.humanbrainproject.knowledgegraph.annotations.NoTests;

/**
 * This is a marker interface to declare explicitly if a method provides unauthorized access to the database.
 */
@NoTests(NoTests.NO_LOGIC)
public @interface UnauthorizedAccess {

    String value();
}
