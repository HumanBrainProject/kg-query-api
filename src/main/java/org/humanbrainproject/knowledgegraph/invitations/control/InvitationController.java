package org.humanbrainproject.knowledgegraph.invitations.control;

import org.humanbrainproject.knowledgegraph.commons.authorization.control.UserInformation;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusInstanceReference;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusSchemaReference;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class InvitationController {

    public boolean hasInvitation(UserInformation user, NexusInstanceReference instanceReference, String queryId){
        //TODO validate in DB
        return true;
    }


    public Set<NexusInstanceReference> getInvitations(UserInformation user, String queryId){
        Set<NexusInstanceReference> result = new HashSet<>();
        result.add(new NexusInstanceReference(new NexusSchemaReference("minds", "core", "dataset", "v1.0.0"), "c171a5fa-4c3d-4d65-b099-64e38c6865f9"));
        result.add(new NexusInstanceReference(new NexusSchemaReference("minds", "core", "dataset", "v1.0.0"), "015d4210-99eb-41a9-b554-58a179411f95"));
        return result;
    }

}
