package org.humanbrainproject.knowledgegraph.commons.authorization.entity;

import java.util.Objects;

public class OidcAccessToken {

    private String token;

    public OidcAccessToken() {
    }

    public OidcAccessToken setToken(String token) {
        this.token = token;
        return this;
    }

    public String getToken() {
        return token;
    }

    public String getBearerToken(){
        return this.token != null ? this.token.toLowerCase().startsWith("bearer ") ? this.token : String.format("Bearer %s", this.token) : null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OidcAccessToken that = (OidcAccessToken) o;
        return Objects.equals(token, that.token);
    }

    @Override
    public int hashCode() {

        return Objects.hash(token);
    }
}
