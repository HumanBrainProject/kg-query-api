package org.humanbrainproject.knowledgegraph.commons.authorization.control;

import org.humanbrainproject.knowledgegraph.commons.authorization.entity.Credential;
import org.humanbrainproject.knowledgegraph.commons.authorization.entity.InternalMasterKey;
import org.humanbrainproject.knowledgegraph.commons.authorization.entity.OidcAccessToken;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.mock.http.client.MockClientHttpResponse;

import java.io.IOException;

import static org.junit.Assert.*;

public class AuthorizationControllerTest {

    AuthorizationController controller;

    @Before
    public void setup(){
        controller = new AuthorizationController();
    }

    @Test
    public void getInterceptorForOIDC() throws IOException {
        //given
        Credential credential = new OidcAccessToken().setToken("foo");
        ClientHttpRequestInterceptor interceptor = controller.getInterceptor(credential);
        HttpRequest request = new MockClientHttpRequest();

        //when
        interceptor.intercept(request, null, new ClientHttpRequestExecution() {
            @Override
            public ClientHttpResponse execute(HttpRequest httpRequest, byte[] bytes) throws IOException {

                //then
                HttpHeaders headers = httpRequest.getHeaders();
                assertEquals("Bearer foo", headers.getFirst("Authorization"));
                return null;
            }
        });
    }


    @Test
    public void getInterceptorForMasterKey() throws IOException {
        //given
        controller.systemOidcHeaderInterceptor = Mockito.spy(new SystemOidcHeaderInterceptor());
        Mockito.doReturn(new OidcAccessToken().setToken("master")).when(controller.systemOidcHeaderInterceptor).getToken();
        Credential credential = new InternalMasterKey();
        ClientHttpRequestInterceptor interceptor = controller.getInterceptor(credential);
        HttpRequest request = new MockClientHttpRequest();

        //when
        interceptor.intercept(request, null, new ClientHttpRequestExecution() {
            @Override
            public ClientHttpResponse execute(HttpRequest httpRequest, byte[] bytes) throws IOException {

                //then
                HttpHeaders headers = httpRequest.getHeaders();
                assertEquals("Bearer master", headers.getFirst("Authorization"));
                return new MockClientHttpResponse(new byte[0], HttpStatus.OK);
            }
        });
    }

}