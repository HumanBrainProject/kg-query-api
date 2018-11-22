package org.humanbrainproject.knowledgegraph.commons.authorization.control;

import org.apache.commons.collections4.map.LRUMap;
import org.humanbrainproject.knowledgegraph.commons.authorization.entity.AccessRight;
import org.humanbrainproject.knowledgegraph.commons.authorization.entity.OidcAccessToken;
import org.humanbrainproject.knowledgegraph.commons.nexus.control.NexusClient;
import org.humanbrainproject.knowledgegraph.commons.nexus.control.NexusConfiguration;
import org.humanbrainproject.knowledgegraph.commons.vocabulary.ArangoVocabulary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class AuthorizationController {

    @Autowired
    NexusClient nexusClient;

    @Autowired
    NexusConfiguration nexusConfiguration;

    private final LRUMap<OidcAccessToken, Set<AccessRight>> tokenToAccessRights = new LRUMap<>();

    public Set<AccessRight> getAccessRights(OidcAccessToken authorizationToken) {
        if (tokenToAccessRights.containsKey(authorizationToken)) {
            return tokenToAccessRights.get(authorizationToken);
        }
        Set<String> allOrganizations = nexusClient.getAllOrganizations(authorizationToken);
        //TODO right now, we only have the differentiation if a organization is visible or not - we therefore only can tell that there is at least READ access. We should have other means to ensure WRITE access.
        Set<AccessRight> accessRights = allOrganizations.stream().map(org -> new AccessRight(org.replace(nexusConfiguration.getNexusBase(NexusConfiguration.ResourceType.ORGANIZATION)+"/", ""), AccessRight.Permission.READ)).collect(Collectors.toSet());
        tokenToAccessRights.put(authorizationToken, accessRights);
        return accessRights;
    }


    public Set<String> getReadableOrganizations(OidcAccessToken authorizationToken){
        return getReadableOrganizations(authorizationToken, null);
    }

    public Set<String> getReadableOrganizations(OidcAccessToken authorizationToken, List<String> whitelistedOrganizations){
        Set<AccessRight> accessRights = getAccessRights(authorizationToken);
        Set<String> readableOrganizations = accessRights.stream().map(AccessRight::getPath).collect(Collectors.toSet());
        if(whitelistedOrganizations!=null){
            readableOrganizations.retainAll(whitelistedOrganizations);
        }
        return readableOrganizations;
    }

    public boolean isReadable(Map data, OidcAccessToken oidcAccessToken){
        if(data.containsKey(ArangoVocabulary.PERMISSION_GROUP) && data.get(ArangoVocabulary.PERMISSION_GROUP) instanceof String){
            return getReadableOrganizations(oidcAccessToken, null).contains(data.get(ArangoVocabulary.PERMISSION_GROUP));
        }
        return false;
    }


    public void flushAccessRights(){
        this.tokenToAccessRights.clear();
    }


}
