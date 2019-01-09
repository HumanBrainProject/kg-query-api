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

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This is a {@link RequestContext} which holds the authorization information provided by the user (population of the values happens as part of the API declaration -> (see e.g. {@link org.humanbrainproject.knowledgegraph.query.api.QueryAPI}).
 * It does not include real logic itself but rather delegates it to its according controller (see {@link AuthorizationController}), provides convenience methods for simplification of parameter passing and declares default fallback values.
 */
@NoTests(NoTests.TRIVIAL)
@RequestContext
public class AuthorizationContext {
    @Autowired
    AuthorizationController authorizationController;

    private Credential credential;

    private Client client;

    public void setMasterCredential() {
        this.credential = new InternalMasterKey();
    }

    public void setCredential(String oidcToken) {
        this.credential = new OidcAccessToken().setToken(oidcToken);
    }

    public Credential getCredential() {
        return credential;
    }

    public Set<String> getReadableOrganizations(){
        return authorizationController.getReadableOrganizations(getCredential(), null);
    }

    public Set<String> getReadableOrganizations(List<String> whitelistedOrganizations) {
        return authorizationController.getReadableOrganizations(getCredential(), whitelistedOrganizations);
    }

    public void populateAuthorizationContext(String authorizationToken) {
        setCredential(authorizationToken);
    }

    public void populateAuthorizationContext(String authorizationToken, Client client) {
        setCredential(authorizationToken);
        this.client = client;
    }

    public Client getClient() {
        return client;
    }

    public SubSpace getSubspace(){
        return getClient() != null ? getClient().getSubSpace() : SubSpace.MAIN;
    }

    public boolean isReadable(Map data) {
        return authorizationController.isReadable(data, getCredential());
    }

    public ClientHttpRequestInterceptor getInterceptor(){
        return authorizationController.getInterceptor(getCredential());
    }
}
