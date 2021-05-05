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

package org.humanbrainproject.knowledgegraph.scopes.control;

import org.humanbrainproject.knowledgegraph.commons.authorization.control.SystemOidcClient;
import org.humanbrainproject.knowledgegraph.commons.authorization.control.UserInformation;
import org.humanbrainproject.knowledgegraph.commons.entity.JsonLdObject;
import org.humanbrainproject.knowledgegraph.commons.jsonld.control.InstanceController;
import org.humanbrainproject.knowledgegraph.commons.nexus.control.NexusConfiguration;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.EqualsFilter;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.ReferenceEqualsFilter;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.AbsoluteNexusInstanceReference;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusInstanceReference;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusSchemaReference;
import org.humanbrainproject.knowledgegraph.scopes.entity.Invitation;
import org.humanbrainproject.knowledgegraph.users.control.UserController;
import org.humanbrainproject.knowledgegraph.users.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class InvitationController extends InstanceController<Invitation> {

    @Autowired
    NexusConfiguration nexusConfiguration;

    @Autowired
    UserController userController;

    @Autowired
    SystemOidcClient oidcClient;

    public Set<User> getInvitedUsersByInstance(NexusInstanceReference instanceReference){
        List<Invitation> invitations = this.listInstances(Collections.singletonList(new ReferenceEqualsFilter(Invitation.INSTANCE_FIELDNAME, nexusConfiguration.getAbsoluteUrl(instanceReference))), Invitation.STRUCTURE, false);
        return invitations.stream().map(i -> userController.getInstance(i.getUser(), User.STRUCTURE)).collect(Collectors.toSet());
    }

    public Set<NexusInstanceReference> getInvitations(User user){
        List<Invitation> invitations = this.listInstances(Collections.singletonList(new ReferenceEqualsFilter(Invitation.USER_FIELDNAME, nexusConfiguration.getAbsoluteUrl(user.getInstanceReference()))), Invitation.STRUCTURE, true);
        return invitations.stream().map(i -> i.getInstance().getInstanceReference()).collect(Collectors.toSet());
    }

    public void removeInvitation(AbsoluteNexusInstanceReference user, AbsoluteNexusInstanceReference instance){
        List<Invitation> invitations = this.listInstances(Arrays.asList(new ReferenceEqualsFilter(Invitation.USER_FIELDNAME, user.getAbsoluteUrl()), new ReferenceEqualsFilter(Invitation.INSTANCE_FIELDNAME, instance.getAbsoluteUrl())), Invitation.STRUCTURE, false);
        for (Invitation invitation : invitations) {
            removeInstance(invitation);
        }
    }


}
