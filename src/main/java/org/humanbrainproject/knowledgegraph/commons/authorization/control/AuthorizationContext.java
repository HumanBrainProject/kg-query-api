package org.humanbrainproject.knowledgegraph.commons.authorization.control;

import org.humanbrainproject.knowledgegraph.commons.api.Client;
import org.humanbrainproject.knowledgegraph.commons.authorization.entity.Credential;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.entity.SubSpace;
import org.springframework.http.client.ClientHttpRequestInterceptor;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface AuthorizationContext {
    void setMasterCredential();

    void setCredential(String oidcToken);

    Credential getCredential();

    Set<String> getReadableOrganizations();

    Set<String> getReadableOrganizations(List<String> whitelistedOrganizations);

    void populateAuthorizationContext(String authorizationToken);

    void populateAuthorizationContext(String authorizationToken, Client client);

    Client getClient();

    SubSpace getSubspace();

    boolean isReadable(Map data);

    ClientHttpRequestInterceptor getInterceptor();
}
