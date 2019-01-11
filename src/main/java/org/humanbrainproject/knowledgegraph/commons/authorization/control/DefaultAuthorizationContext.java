package org.humanbrainproject.knowledgegraph.commons.authorization.control;

import org.humanbrainproject.knowledgegraph.annotations.NoTests;
import org.humanbrainproject.knowledgegraph.annotations.RequestContext;
import org.humanbrainproject.knowledgegraph.commons.api.Client;
import org.humanbrainproject.knowledgegraph.commons.authorization.entity.Credential;
import org.humanbrainproject.knowledgegraph.commons.authorization.entity.InternalMasterKey;
import org.humanbrainproject.knowledgegraph.commons.authorization.entity.OidcAccessToken;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.entity.SubSpace;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This is a {@link RequestContext} which holds the authorization information provided by the user (population of the values happens as part of the API declaration -> (see e.g. {@link org.humanbrainproject.knowledgegraph.query.api.QueryAPI}).
 * It does not include real logic itself but rather delegates it to its according controller (see {@link AuthorizationController}), provides convenience methods for simplification of parameter passing and declares default fallback values.
 */
@NoTests(NoTests.TRIVIAL)
@RequestContext
@Component
public class DefaultAuthorizationContext implements AuthorizationContext {
    @Autowired
    AuthorizationController authorizationController;

    private Credential credential;

    private Client client;

    @Override
    public void setMasterCredential() {
        this.credential = new InternalMasterKey();
    }

    @Override
    public void setCredential(String oidcToken) {
        this.credential = new OidcAccessToken().setToken(oidcToken);
    }

    @Override
    public Credential getCredential() {
        return credential;
    }

    @Override
    public Set<String> getReadableOrganizations(){
        return authorizationController.getReadableOrganizations(getCredential(), null);
    }

    @Override
    public Set<String> getReadableOrganizations(List<String> whitelistedOrganizations) {
        return authorizationController.getReadableOrganizations(getCredential(), whitelistedOrganizations);
    }

    @Override
    public void populateAuthorizationContext(String authorizationToken) {
        setCredential(authorizationToken);
    }

    @Override
    public void populateAuthorizationContext(String authorizationToken, Client client) {
        setCredential(authorizationToken);
        this.client = client;
    }

    @Override
    public Client getClient() {
        return client;
    }

    @Override
    public SubSpace getSubspace(){
        return getClient() != null ? getClient().getSubSpace() : SubSpace.MAIN;
    }

    @Override
    public boolean isReadable(Map data) {
        return authorizationController.isReadable(data, getCredential());
    }

    @Override
    public ClientHttpRequestInterceptor getInterceptor(){
        return authorizationController.getInterceptor(getCredential());
    }
}
