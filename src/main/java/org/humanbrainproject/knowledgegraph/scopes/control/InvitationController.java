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
