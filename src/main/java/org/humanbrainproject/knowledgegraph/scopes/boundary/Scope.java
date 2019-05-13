package org.humanbrainproject.knowledgegraph.scopes.boundary;

import org.humanbrainproject.knowledgegraph.commons.authorization.control.SystemOidcClient;
import org.humanbrainproject.knowledgegraph.commons.authorization.control.UserInformation;
import org.humanbrainproject.knowledgegraph.commons.authorization.entity.Credential;
import org.humanbrainproject.knowledgegraph.commons.authorization.entity.OidcAccessToken;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusInstanceReference;
import org.humanbrainproject.knowledgegraph.invitations.control.InvitationController;
import org.humanbrainproject.knowledgegraph.scopes.control.ScopeEvaluator;
import org.ietf.jgss.Oid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
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

    public Set<String> getIdWhitelistForUser(NexusInstanceReference reference, String query, OidcAccessToken accessToken) {
        UserInformation userInfo = oidcClient.getUserInfo(accessToken);
        boolean hasInvitation = invitationController.hasInvitation(userInfo, reference, query);
        if(hasInvitation) {
            return scopeEvaluator.getScope(Collections.singleton(reference), query);
        }
        return Collections.emptySet();
    }

    public Set<String> getIdWhitelistForUser(String query, Credential credential){
        if(credential instanceof OidcAccessToken) {
            UserInformation userInfo = oidcClient.getUserInfo((OidcAccessToken)credential);
            Set<NexusInstanceReference> invitations = invitationController.getInvitations(userInfo, query);
            if (invitations != null && !invitations.isEmpty()) {
                return scopeEvaluator.getScope(invitations, query);
            }
        }
        return Collections.emptySet();
    }


}
