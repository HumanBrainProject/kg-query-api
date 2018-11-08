package org.humanbrainproject.knowledgegraph.commons.authorization.control;

import org.humanbrainproject.knowledgegraph.commons.authorization.entity.OidcAccessToken;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.util.Arrays;

public class OidcHeaderInterceptor implements ClientHttpRequestInterceptor {

    private final OidcAccessToken token;
    private final String contentType;

    public OidcHeaderInterceptor(OidcAccessToken token) {
        this(token, MediaType.APPLICATION_JSON);
    }

    public OidcHeaderInterceptor(OidcAccessToken token, String contentType) {
        this.token = token;
        this.contentType = contentType;
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest httpRequest, byte[] bytes, ClientHttpRequestExecution clientHttpRequestExecution) throws IOException {
        httpRequest.getHeaders().set(HttpHeaders.CONTENT_TYPE, contentType);
        httpRequest.getHeaders().setAccept(Arrays.asList(org.springframework.http.MediaType.APPLICATION_JSON, org.springframework.http.MediaType.parseMediaType("application/ld+json")));
        if (this.token != null) {
            httpRequest.getHeaders().set(HttpHeaders.AUTHORIZATION, token.getBearerToken());
        }
        return clientHttpRequestExecution.execute(httpRequest, bytes);
    }
}