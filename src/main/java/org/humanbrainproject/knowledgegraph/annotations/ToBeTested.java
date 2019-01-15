package org.humanbrainproject.knowledgegraph.annotations;

public @interface ToBeTested {

    String value() default "";
    boolean integrationTestRequired() default false;
    boolean systemTestRequired() default false;

    boolean easy() default false;
}
