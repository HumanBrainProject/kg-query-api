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

import org.humanbrainproject.knowledgegraph.commons.authorization.entity.AccessRight;
import org.humanbrainproject.knowledgegraph.commons.authorization.entity.Credential;
import org.humanbrainproject.knowledgegraph.commons.authorization.entity.InternalMasterKey;
import org.humanbrainproject.knowledgegraph.commons.authorization.entity.OidcAccessToken;
import org.humanbrainproject.knowledgegraph.testFactory.TestObjectFactory;
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
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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

    @Test
    public void getAccessRights(){
        //given
        OidcAccessToken token = new OidcAccessToken();
        AccessRight right = new AccessRight("foo", AccessRight.Permission.READ);
        token.setToken("foobar");
        controller.tokenToAccessRights.put(token.getBearerToken(), new HashSet<>(Arrays.asList(right)));


        //when
        Set<AccessRight> accessRights = controller.getAccessRights(token);

        //then
        assertEquals(1, accessRights.size());
        AccessRight firstRight = accessRights.iterator().next();
        assertEquals("foo", firstRight.getPath());
        assertTrue(firstRight.isReadOnly());
        assertFalse(firstRight.canWrite());
    }


    @Test
    public void getReadableOrganizationsNullWhitelist(){
        //given
        OidcAccessToken token = new OidcAccessToken();
        token.setToken("foobar");
        AccessRight right = new AccessRight("foo", AccessRight.Permission.READ);
        controller.tokenToAccessRights.put(token.getBearerToken(), new HashSet<>(Arrays.asList(right)));

        //when
        Set<String> readableOrganizations = controller.getReadableOrganizations(token, null);

        //then
        assertEquals(1, readableOrganizations.size());
        assertEquals("foo", readableOrganizations.iterator().next());
    }

    @Test
    public void getReadableOrganizationsWithWhitelistMatch(){
        //given
        OidcAccessToken token = new OidcAccessToken();
        AccessRight right = new AccessRight("foo", AccessRight.Permission.READ);
        controller.tokenToAccessRights.put(token.getBearerToken(), new HashSet<>(Arrays.asList(right)));

        //when
        Set<String> readableOrganizations = controller.getReadableOrganizations(token, Arrays.asList("foo", "bar"));

        //then
        assertEquals(1, readableOrganizations.size());
        assertEquals("foo", readableOrganizations.iterator().next());
    }

    @Test
    public void getReadableOrganizationsWithWhitelistNoMatch(){
        //given
        OidcAccessToken token = new OidcAccessToken();
        token.setToken("foobar");
        AccessRight right = new AccessRight("foo", AccessRight.Permission.READ);
        controller.tokenToAccessRights.put(token.getBearerToken(), new HashSet<>(Arrays.asList(right)));

        //when
        Set<String> readableOrganizations = controller.getReadableOrganizations(token, Arrays.asList("bar"));

        //then
        assertEquals(0, readableOrganizations.size());
    }


    @Test
    public void isReadableTrue(){
        //given
        Map<String, Object> arangoInstance = TestObjectFactory.createArangoInstanceSkeleton("fooinstance", "foopermission");
        OidcAccessToken token = new OidcAccessToken();
        token.setToken("foobar");
        AccessRight right = new AccessRight("foopermission", AccessRight.Permission.READ);
        controller.tokenToAccessRights.put(token.getBearerToken(), new HashSet<>(Arrays.asList(right)));

        //when
        boolean readable = controller.isReadable(arangoInstance, token);

        //then
        assertTrue(readable);
    }

    @Test
    public void isReadableFalse(){
        //given
        Map<String, Object> arangoInstance = TestObjectFactory.createArangoInstanceSkeleton("fooinstance", "foopermission");
        OidcAccessToken token = new OidcAccessToken();
        token.setToken("foobar");
        AccessRight right = new AccessRight("barpermission", AccessRight.Permission.READ);
        controller.tokenToAccessRights.put(token.getBearerToken(), new HashSet<>(Arrays.asList(right)));

        //when
        boolean readable = controller.isReadable(arangoInstance, token);

        //then
        assertFalse(readable);
    }
}