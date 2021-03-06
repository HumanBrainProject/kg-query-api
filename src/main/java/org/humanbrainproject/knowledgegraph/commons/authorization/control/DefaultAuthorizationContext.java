/*
 * Copyright 2018 - 2021 Swiss Federal Institute of Technology Lausanne (EPFL)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * This open source software code was developed in part or in whole in the
 * Human Brain Project, funded from the European Union's Horizon 2020
 * Framework Programme for Research and Innovation under
 * Specific Grant Agreements No. 720270, No. 785907, and No. 945539
 * (Human Brain Project SGA1, SGA2 and SGA3).
 *
 */

package org.humanbrainproject.knowledgegraph.commons.authorization.control;

import org.humanbrainproject.knowledgegraph.annotations.NoTests;
import org.humanbrainproject.knowledgegraph.commons.api.Client;
import org.humanbrainproject.knowledgegraph.commons.authorization.entity.Credential;
import org.humanbrainproject.knowledgegraph.commons.authorization.entity.InternalMasterKey;
import org.humanbrainproject.knowledgegraph.commons.authorization.entity.OidcAccessToken;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.entity.SubSpace;
import org.humanbrainproject.knowledgegraph.scopes.boundary.Scope;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This is a {@link RequestScope}d bean which holds the authorization information provided by the user (population of the values happens as part of the API declaration -> (see e.g. {@link org.humanbrainproject.knowledgegraph.query.api.QueryAPI}).
 * It does not include real logic itself but rather delegates it to its according controller (see {@link AuthorizationController}), provides convenience methods for simplification of parameter passing and declares default fallback values.
 */
@NoTests(NoTests.TRIVIAL)
@Component
@RequestScope
public class DefaultAuthorizationContext implements AuthorizationContext {

    @Autowired
    Scope scope;

    @Autowired
    SystemOidcClient oidcClient;

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
    public String getUserId(){
        return authorizationController.getUserId(getCredential());
    }

    @Override
    public Set<String> getReadableOrganizations(){
        //WARNING: Make sure, that when giving access to the full blown organization list, make sure, the access to the database is restricted to RELEASED ONLY! (handled in the org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.ArangoDatabaseFactory
        if(isAllowedToSeeReleasedInstancesOnly()){
            //We ensure the access to the released database in a different place. If somebody has access to the released space only, we will provide it for all spaces.
            return authorizationController.getReadableOrganizations(new InternalMasterKey(), null);
        }
        return authorizationController.getReadableOrganizations(getCredential(), null);
    }

    @Override
    public Set<String> getReadableOrganizations(Credential credential, List<String> whitelistedOrganizations){
        //WARNING: Make sure, that when giving access to the full blown organization list, make sure, the access to the database is restricted to RELEASED ONLY! (handled in the org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.ArangoDatabaseFactory
        if(isAllowedToSeeReleasedInstancesOnly()){
            //We ensure the access to the released database in a different place. If somebody has access to the released space only, we will provide it for all spaces.
            return authorizationController.getReadableOrganizations(new InternalMasterKey(), whitelistedOrganizations);
        }
        return authorizationController.getReadableOrganizations(credential, whitelistedOrganizations);
    }

    @Override
    public Set<String> getReadableOrganizations(List<String> whitelistedOrganizations) {
        //WARNING: Make sure, that when giving access to the full blown organization list, make sure, the access to the database is restricted to RELEASED ONLY! (handled in the org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.ArangoDatabaseFactory
        if(isAllowedToSeeReleasedInstancesOnly()){
            //We ensure the access to the released database in a different place. If somebody has access to the released space only, we will provide it for all spaces.
            return authorizationController.getReadableOrganizations(new InternalMasterKey(), whitelistedOrganizations);
        }
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

    @Override
    public Set<String> getInvitations(String query) {
        return scope.getIdWhitelistForUser(query, getCredential());
    }


    @Override
    public boolean isAllowedToSeeReleasedInstancesOnly() {
        Credential c = getCredential();
        if(c instanceof OidcAccessToken) {
            UserInformation userInfo = oidcClient.getUserInfo(((OidcAccessToken) c));
            return !userInfo.hasCuratedPermission() && userInfo.hasReleasedPermission();
        }
        else return false;
    }
}
