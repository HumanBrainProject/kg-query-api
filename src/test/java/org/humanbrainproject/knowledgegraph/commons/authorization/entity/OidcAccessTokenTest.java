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