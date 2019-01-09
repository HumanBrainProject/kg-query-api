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