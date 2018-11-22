package org.humanbrainproject.knowledgegraph.commons.authorization.control;

import org.humanbrainproject.knowledgegraph.commons.authorization.entity.OidcAccessToken;
import org.springframework.http.client.ClientHttpRequestInterceptor;

public interface TokenBasedClientHttpRequestInterceptor extends ClientHttpRequestInterceptor{

    OidcAccessToken getToken();
}
