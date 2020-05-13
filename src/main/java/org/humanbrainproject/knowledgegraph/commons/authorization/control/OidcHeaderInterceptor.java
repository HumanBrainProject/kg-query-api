/*
 * Copyright 2020 EPFL/Human Brain Project PCO
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.humanbrainproject.knowledgegraph.commons.authorization.control;

import org.humanbrainproject.knowledgegraph.annotations.Tested;
import org.humanbrainproject.knowledgegraph.commons.authorization.entity.OidcAccessToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;

@Tested
public class OidcHeaderInterceptor implements ClientHttpRequestInterceptor {

    private final OidcAccessToken token;
    private final String contentType;

    protected Logger logger = LoggerFactory.getLogger(ClientHttpRequestInterceptor.class);

    OidcHeaderInterceptor(OidcAccessToken token) {
        this(token, MediaType.APPLICATION_JSON);
    }

    private OidcHeaderInterceptor(OidcAccessToken token, String contentType) {
        this.token = token;
        this.contentType = contentType;
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest httpRequest, byte[] bytes, ClientHttpRequestExecution clientHttpRequestExecution) throws IOException {
        if(httpRequest.getMethod()!= HttpMethod.GET) {
            logger.info(String.format("%s to %s", httpRequest.getMethod().name(), httpRequest.getURI()));
        }

        httpRequest.getHeaders().setAcceptCharset(Collections.singletonList(StandardCharsets.UTF_8));
        httpRequest.getHeaders().set(HttpHeaders.CONTENT_TYPE, contentType);
        httpRequest.getHeaders().setAccept(Arrays.asList(org.springframework.http.MediaType.APPLICATION_JSON, org.springframework.http.MediaType.parseMediaType("application/ld+json")));
        if (this.token != null && token.getBearerToken()!=null) {
            httpRequest.getHeaders().set(HttpHeaders.AUTHORIZATION, token.getBearerToken());
        }
        ClientHttpResponse execute = clientHttpRequestExecution.execute(httpRequest, bytes);
        return execute;
    }
}
