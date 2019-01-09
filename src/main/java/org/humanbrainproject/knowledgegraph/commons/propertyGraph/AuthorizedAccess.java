package org.humanbrainproject.knowledgegraph.commons.propertyGraph;

import org.humanbrainproject.knowledgegraph.annotations.NoTests;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * This is a marker interface to declare explicitly if a method requires authorization. There is no logic yet to validate if it actually does it yet.
 */

@NoTests(NoTests.NO_LOGIC)
@Target(ElementType.METHOD)
public @interface AuthorizedAccess {

    String value() default "";

}

