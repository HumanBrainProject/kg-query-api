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

package org.humanbrainproject.knowledgegraph.scopes.boundary;

import org.humanbrainproject.knowledgegraph.commons.authorization.control.SystemOidcClient;
import org.humanbrainproject.knowledgegraph.commons.authorization.control.UserInformation;
import org.humanbrainproject.knowledgegraph.commons.authorization.entity.Credential;
import org.humanbrainproject.knowledgegraph.commons.authorization.entity.OidcAccessToken;
import org.humanbrainproject.knowledgegraph.commons.nexus.control.NexusConfiguration;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.EqualsFilter;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.AbsoluteNexusInstanceReference;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusInstanceReference;
import org.humanbrainproject.knowledgegraph.scopes.control.InvitationController;
import org.humanbrainproject.knowledgegraph.scopes.control.ScopeEvaluator;
import org.humanbrainproject.knowledgegraph.scopes.entity.Invitation;
import org.humanbrainproject.knowledgegraph.scopes.entity.InvitedUser;
import org.humanbrainproject.knowledgegraph.users.control.UserController;
import org.humanbrainproject.knowledgegraph.users.entity.UserByName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class Scope {

    @Autowired
    ScopeEvaluator scopeEvaluator;

    @Autowired
    SystemOidcClient oidcClient;

    @Autowired
    InvitationController invitationController;

    @Autowired
    UserController userController;

    @Autowired
    NexusConfiguration configuration;


    public Set<InvitedUser> getInvitedUsersForId(NexusInstanceReference instanceReference){
        Set<UserByName> invitedUsersByInstance = invitationController.getInvitedUsersByInstance(instanceReference);
        return invitedUsersByInstance.stream().map(u -> {
            InvitedUser invitedUser = new InvitedUser();
            invitedUser.setUserName(u.getUserName());
            return invitedUser;
        }).collect(Collectors.toSet());
    }

    public Set<String> getIdWhitelistForUser(String query, Credential credential){
        if(credential instanceof OidcAccessToken) {
            UserInformation userInfo = oidcClient.getUserInfo((OidcAccessToken)credential);
            UserByName user = userController.findUniqueInstance(Collections.singletonList(new EqualsFilter(UserByName.USER_NAME_FIELD, userInfo.getUserName())), UserByName.STRUCTURE, true);
            Set<NexusInstanceReference> invitations = user==null ? null : invitationController.getInvitations(user);
            if (invitations != null && !invitations.isEmpty()) {
                return scopeEvaluator.getScope(invitations, query);
            }
        }
        return Collections.emptySet();
    }

    public void removeScopeFromUser(NexusInstanceReference instanceReference, String userName){
        UserByName user = userController.findUniqueInstance(Collections.singletonList(new EqualsFilter(UserByName.USER_NAME_FIELD, userName)), UserByName.STRUCTURE, true);
        if(user!=null) {
            invitationController.removeInvitation(new AbsoluteNexusInstanceReference(user.getInstanceReference(), configuration), new AbsoluteNexusInstanceReference(instanceReference, configuration));
        }
    }

    public void addScopeToUser(NexusInstanceReference instanceReference, String userName){
        UserByName user = userController.findUniqueInstance(Collections.singletonList(new EqualsFilter(UserByName.USER_NAME_FIELD, userName)), UserByName.STRUCTURE, true);
        if(user==null){
            //Invited user does not yet exist.
            user = userController.createInstance(new UserByName(userName));
        }
        Set<NexusInstanceReference> invitations = invitationController.getInvitations(user);
        if(!invitations.contains(instanceReference)) {
            Invitation invitation = new Invitation(new AbsoluteNexusInstanceReference(user.getInstanceReference(), configuration), new AbsoluteNexusInstanceReference(instanceReference, configuration));
            invitationController.createInstance(invitation);
        }

    }
}
