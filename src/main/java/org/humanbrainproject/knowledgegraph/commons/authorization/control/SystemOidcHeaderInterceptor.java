package org.humanbrainproject.knowledgegraph.commons.authorization.control;

import org.humanbrainproject.knowledgegraph.commons.authorization.entity.OidcAccessToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;

@Component
public class SystemOidcHeaderInterceptor implements ClientHttpRequestInterceptor {

    @Autowired
    SystemOidcClient client;



    protected Logger logger = LoggerFactory.getLogger(ClientHttpRequestInterceptor.class);

    public OidcAccessToken getToken() {
        return client.getAuthorizationToken();
    }

    private void setAuthTokenToRequest(HttpRequest request) {
        OidcAccessToken token = getToken();
        if (token != null) {
            request.getHeaders().add("Authorization", token.getBearerToken());
        }
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest httpRequest, byte[] bytes, ClientHttpRequestExecution clientHttpRequestExecution) throws IOException {
        if(httpRequest.getMethod()!= HttpMethod.GET) {
            logger.info(String.format("%s to %s", httpRequest.getMethod().name(), httpRequest.getURI()));
        }
        setAuthTokenToRequest(httpRequest);
        httpRequest.getHeaders().setAcceptCharset(Collections.singletonList(StandardCharsets.UTF_8));
        httpRequest.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        httpRequest.getHeaders().setAccept(Arrays.asList(MediaType.APPLICATION_JSON, MediaType.parseMediaType("application/ld+json")));
        ClientHttpResponse response = clientHttpRequestExecution.execute(httpRequest, bytes);
        if (response.getStatusCode() == HttpStatus.UNAUTHORIZED) {
            //The token seems to have timed out - let's try to refresh it and reexecute the request
            client.refreshToken();
            setAuthTokenToRequest(httpRequest);
            response = clientHttpRequestExecution.execute(httpRequest, bytes);
        }
        return response;
    }
}
