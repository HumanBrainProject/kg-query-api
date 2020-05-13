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

package org.humanbrainproject.knowledgegraph.commons.authorization.entity;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class OidcAccessTokenTest {

    OidcAccessToken oidcAccessToken;

    @Before
    public void setup(){
        oidcAccessToken = new OidcAccessToken();
    }

    @Test
    public void getBearerTokenWithPrefix() {
        //given
        oidcAccessToken.setToken("Bearer foo");

        //when
        String bearerToken = oidcAccessToken.getBearerToken();

        //then
        assertEquals("Bearer foo", bearerToken);
    }

    @Test
    public void getBearerTokenWithLowercasePrefix() {
        //given
        oidcAccessToken.setToken("bearer foo");

        //when
        String bearerToken = oidcAccessToken.getBearerToken();

        //then
        assertEquals("Bearer foo", bearerToken);
    }

    @Test
    public void getBearerTokenWithoutPrefix() {
        //given
        oidcAccessToken.setToken("foo");

        //when
        String bearerToken = oidcAccessToken.getBearerToken();

        //then
        assertEquals("Bearer foo", bearerToken);
    }
}