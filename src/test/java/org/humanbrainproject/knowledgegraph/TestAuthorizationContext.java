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

    @Override
    public String getUserId() {
        return null;
    }

    @Override
    public Set<String> getReadableOrganizations(Credential credential, List<String> whitelistedOrganizations) {
        return null;
    }

    @Override
    public Set<String> getInvitations(String query) {
        return null;
    }

    @Override
    public boolean isAllowedToSeeReleasedInstancesOnly() {
        return false;
    }
}
