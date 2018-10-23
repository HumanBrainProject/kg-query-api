package org.humanbrainproject.knowledgegraph.nexus.control;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import javax.ws.rs.core.MediaType;
import java.io.IOException;

public class OidcHeaderInterceptor implements ClientHttpRequestInterceptor {

    private final String token;
    private final String contentType;

    public OidcHeaderInterceptor(String token) {
        this(token, MediaType.APPLICATION_JSON);
    }

    public OidcHeaderInterceptor(String token, String contentType) {
        this.token = token;
        this.contentType = contentType;
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest httpRequest, byte[] bytes, ClientHttpRequestExecution clientHttpRequestExecution) throws IOException {
        httpRequest.getHeaders().set(HttpHeaders.CONTENT_TYPE, contentType);
        if (this.token != null) {
            httpRequest.getHeaders().set(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", token));
        }
        return clientHttpRequestExecution.execute(httpRequest, bytes);
    }
}
