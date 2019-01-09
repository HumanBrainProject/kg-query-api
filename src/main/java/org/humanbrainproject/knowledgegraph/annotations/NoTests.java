package org.humanbrainproject.knowledgegraph.annotations;

public @interface NoTests {

    String value();

    String TRIVIAL = "This code is too trivial to test";
    String NO_LOGIC = "This code doesn't involve logic (e.g. simple bean, constants)";
}
