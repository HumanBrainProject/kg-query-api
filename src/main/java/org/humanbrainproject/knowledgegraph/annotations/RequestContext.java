package org.humanbrainproject.knowledgegraph.annotations;

import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

/**
 * A request context is a spring bean with request scope and typically used to keep the state for the current execution context (such as credentials provided by the user)
 */
@RequestScope
@Component
public @interface RequestContext {
}
