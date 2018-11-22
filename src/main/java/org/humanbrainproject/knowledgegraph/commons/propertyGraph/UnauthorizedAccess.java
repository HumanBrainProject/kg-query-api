package org.humanbrainproject.knowledgegraph.commons.propertyGraph;

/**
 * This is a marker interface to declare explicitly if a method provides unauthorized access to the database.
 */
public @interface UnauthorizedAccess {

    String value();
}
