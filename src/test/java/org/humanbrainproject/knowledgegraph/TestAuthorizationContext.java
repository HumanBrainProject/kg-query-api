package org.humanbrainproject.knowledgegraph;

import org.humanbrainproject.knowledgegraph.commons.api.Client;
import org.humanbrainproject.knowledgegraph.commons.authorization.control.AuthorizationContext;
import org.humanbrainproject.knowledgegraph.commons.authorization.entity.Credential;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.entity.SubSpace;
import org.springframework.http.client.ClientHttpRequestInterceptor;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class TestAuthorizationContext implements AuthorizationContext {
    @Override
    public void setMasterCredential() {

    }

    @Override
    public void setCredential(String oidcToken) {

    }

    @Override
    public Credential getCredential() {
        return null;
    }

    @Override
    public Set<String> getReadableOrganizations() {
        return null;
    }

    @Override
    public Set<String> getReadableOrganizations(List<String> whitelistedOrganizations) {
        return null;
    }

    @Override
    public void populateAuthorizationContext(String authorizationToken) {

    }

    @Override
    public void populateAuthorizationContext(String authorizationToken, Client client) {

    }

    @Override
    public Client getClient() {
        return null;
    }

    @Override
    public SubSpace getSubspace() {
        return null;
    }

    @Override
    public boolean isReadable(Map data) {
        return false;
    }

    @Override
    public ClientHttpRequestInterceptor getInterceptor() {
        return null;
    }
}
