package org.humanbrainproject.knowledgegraph.commons;

import org.humanbrainproject.knowledgegraph.annotations.NoTests;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@NoTests(NoTests.NO_LOGIC)
@Retention(RetentionPolicy.RUNTIME)
public @interface ExternalApi {
}
