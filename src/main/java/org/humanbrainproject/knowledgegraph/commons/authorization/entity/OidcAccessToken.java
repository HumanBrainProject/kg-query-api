package org.humanbrainproject.knowledgegraph.commons.authorization.entity;

import org.apache.commons.lang3.StringUtils;
import org.humanbrainproject.knowledgegraph.annotations.Tested;

import java.util.Objects;

@Tested
public class OidcAccessToken implements Credential {

    private String token;

    public OidcAccessToken setToken(String token) {
        this.token = token;
        return this;
    }

    public String getToken() {
        return token;
    }

    public String getBearerToken(){
        return this.token != null ? this.token.toLowerCase().startsWith("bearer ") ? StringUtils.capitalize(this.token) : String.format("Bearer %s", this.token) : null;
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
