package org.humanbrainproject.knowledgegraph.nexus.control;

import org.humanbrainproject.knowledgegraph.authorization.control.OidcClient;
import org.humanbrainproject.knowledgegraph.authorization.entity.OidcAccessToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class SystemOidcHeaderInterceptor implements ClientHttpRequestInterceptor {

    @Autowired
    OidcClient client;

    private OidcAccessToken currentToken = null;

    private OidcAccessToken getToken() throws IOException {
        if (currentToken == null) {
            currentToken = client.getAuthorizationToken();
        }
        return currentToken;
    }

    private void setAuthTokenToRequest(HttpRequest request) throws IOException {
        OidcAccessToken token = getToken();
        if (token != null) {
            request.getHeaders().add("Authorization", token.getBearerToken());
        }
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest httpRequest, byte[] bytes, ClientHttpRequestExecution clientHttpRequestExecution) throws IOException {
        setAuthTokenToRequest(httpRequest);
        ClientHttpResponse response = clientHttpRequestExecution.execute(httpRequest, bytes);
        if (response.getStatusCode() == HttpStatus.UNAUTHORIZED) {
            //The token seems to have timed out - let's try to refresh it and reexecute the request
            currentToken = null;
            setAuthTokenToRequest(httpRequest);
            response = clientHttpRequestExecution.execute(httpRequest, bytes);
        }
        return response;
    }
}
