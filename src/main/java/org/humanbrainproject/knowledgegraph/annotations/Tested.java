package org.humanbrainproject.knowledgegraph.annotations;

/**
 * This is a purely manual tagging annotation allowing the programmer to state the current testing status.
 * This can be subject of change (e.g. if new code is added / code is changed) and therefore has to be
 * checked regularly for validity.
 */
public @interface Tested {

    boolean fullyTested() default true;

}
