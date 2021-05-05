/*
 * Copyright 2018 - 2021 Swiss Federal Institute of Technology Lausanne (EPFL)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * This open source software code was developed in part or in whole in the
 * Human Brain Project, funded from the European Union's Horizon 2020
 * Framework Programme for Research and Innovation under
 * Specific Grant Agreements No. 720270, No. 785907, and No. 945539
 * (Human Brain Project SGA1, SGA2 and SGA3).
 *
 */

package org.humanbrainproject.knowledgegraph.commons.authorization.control;

import org.humanbrainproject.knowledgegraph.commons.authorization.entity.OidcAccessToken;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.mock.http.client.MockClientHttpResponse;

import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.*;

public class OidcHeaderInterceptorTest {

    OidcAccessToken token;

    @Before
    public void setup(){
        this.token = new OidcAccessToken().setToken("fooToken");
    }


    @Test
    public void interceptWithToken() throws IOException {
        //given
        OidcHeaderInterceptor oidcHeaderInterceptor = new OidcHeaderInterceptor(this.token);

        //when
        oidcHeaderInterceptor.intercept(new MockClientHttpRequest(), new byte[0], new ClientHttpRequestExecution() {
            @Override
            public ClientHttpResponse execute(HttpRequest httpRequest, byte[] bytes) throws IOException {

                //then
                assertEquals( MediaType.APPLICATION_JSON, httpRequest.getHeaders().getFirst(HttpHeaders.CONTENT_TYPE));
                assertTrue(httpRequest.getHeaders().getAcceptCharset().contains(StandardCharsets.UTF_8));
                assertTrue(httpRequest.getHeaders().getAccept().contains(org.springframework.http.MediaType.parseMediaType(MediaType.APPLICATION_JSON)));
                assertEquals("Bearer fooToken", httpRequest.getHeaders().getFirst(HttpHeaders.AUTHORIZATION));
                return new MockClientHttpResponse(new byte[0], HttpStatus.OK);
            }
        });
    }


    @Test
    public void interceptWithoutToken() throws IOException {
        //given
        OidcHeaderInterceptor oidcHeaderInterceptor = new OidcHeaderInterceptor(null);

        //when
        oidcHeaderInterceptor.intercept(new MockClientHttpRequest(), new byte[0], new ClientHttpRequestExecution() {
            @Override
            public ClientHttpResponse execute(HttpRequest httpRequest, byte[] bytes) throws IOException {

                //then
                assertFalse(httpRequest.getHeaders().containsKey(HttpHeaders.AUTHORIZATION));
                return new MockClientHttpResponse(new byte[0], HttpStatus.OK);
            }
        });
    }

    @Test
    public void interceptWithNullToken() throws IOException {
        //given
        OidcHeaderInterceptor oidcHeaderInterceptor = new OidcHeaderInterceptor(this.token);
        this.token.setToken(null);

        //when
        oidcHeaderInterceptor.intercept(new MockClientHttpRequest(), new byte[0], new ClientHttpRequestExecution() {
            @Override
            public ClientHttpResponse execute(HttpRequest httpRequest, byte[] bytes) throws IOException {

                //then
                assertFalse(httpRequest.getHeaders().containsKey(HttpHeaders.AUTHORIZATION));
                return new MockClientHttpResponse(new byte[0], HttpStatus.OK);
            }
        });
    }




}