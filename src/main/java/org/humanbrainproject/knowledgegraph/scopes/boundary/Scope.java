/*
 * Copyright 2020 EPFL/Human Brain Project PCO
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
import org.humanbrainproject.knowledgegraph.users.entity.User;
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
        Set<User> invitedUsersByInstance = invitationController.getInvitedUsersByInstance(instanceReference);
        return invitedUsersByInstance.stream().map(u -> {
            InvitedUser invitedUser = new InvitedUser();
            invitedUser.setUserId(u.getUserId());
            return invitedUser;
        }).collect(Collectors.toSet());
    }

    public Set<String> getIdWhitelistForUser(String query, Credential credential){
        if(credential instanceof OidcAccessToken) {
            UserInformation userInfo = oidcClient.getUserInfo((OidcAccessToken)credential);
            User user = userController.findUniqueInstance(Collections.singletonList(new EqualsFilter(User.USER_ID_FIELD, userInfo.getUserId())), User.STRUCTURE, true);
            Set<NexusInstanceReference> invitations = user==null ? null : invitationController.getInvitations(user);
            if (invitations != null && !invitations.isEmpty()) {
                return scopeEvaluator.getScope(invitations, query);
            }
        }
        return Collections.emptySet();
    }

    public void removeScopeFromUser(NexusInstanceReference instanceReference, String userId){
        User user = userController.findUniqueInstance(Collections.singletonList(new EqualsFilter(User.USER_ID_FIELD, userId)), User.STRUCTURE, true);
        if(user!=null) {
            invitationController.removeInvitation(new AbsoluteNexusInstanceReference(user.getInstanceReference(), configuration), new AbsoluteNexusInstanceReference(instanceReference, configuration));
        }
    }

    public void addScopeToUser(NexusInstanceReference instanceReference, String userId){
        User user = userController.findUniqueInstance(Collections.singletonList(new EqualsFilter(User.USER_ID_FIELD, userId)), User.STRUCTURE, true);
        if(user==null){
            //Invited user does not yet exist.
            user = userController.createInstance(new User(userId));
        }
        Set<NexusInstanceReference> invitations = invitationController.getInvitations(user);
        if(!invitations.contains(instanceReference)) {
            Invitation invitation = new Invitation(new AbsoluteNexusInstanceReference(user.getInstanceReference(), configuration), new AbsoluteNexusInstanceReference(instanceReference, configuration));
            invitationController.createInstance(invitation);
        }

    }
}
