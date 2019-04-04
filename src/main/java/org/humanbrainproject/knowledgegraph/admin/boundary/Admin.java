package org.humanbrainproject.knowledgegraph.admin.boundary;

import org.humanbrainproject.knowledgegraph.admin.entity.ACLEntry;
import org.humanbrainproject.knowledgegraph.commons.authorization.control.AuthorizationContext;
import org.humanbrainproject.knowledgegraph.commons.authorization.control.SystemOidcClient;
import org.humanbrainproject.knowledgegraph.commons.authorization.entity.Credential;
import org.humanbrainproject.knowledgegraph.commons.nexus.control.NexusClient;
import org.humanbrainproject.knowledgegraph.commons.nexus.control.NexusConfiguration;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusRelativeUrl;
import org.humanbrainproject.knowledgegraph.query.entity.JsonDocument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class Admin {

    @Autowired
    NexusClient nexusClient;

    @Autowired
    AuthorizationContext context;

    @Autowired
    SystemOidcClient oidcClient;


    public void createPrivateSpace(String spaceName, List<String> managerGroups){
        Credential credential = context.getCredential();
        nexusClient.createOrganization(spaceName, credential);
        nexusClient.setAccessRightsForOrganization(spaceName, managerGroups, credential);
        String serviceAccountSubject = "service-account-" + oidcClient.getClientId();

        nexusClient.createProject(spaceName, "main", credential, "takes care of lifecycle objects (creation / updates are handled by technical users, space administrators can deprecate)", null);
        List<ACLEntry> aclEntriesForMain =  new ArrayList<>();
        aclEntriesForMain.add(new ACLEntry(ACLEntry.Type.SUBJECT, serviceAccountSubject, oidcClient.getRealm(), "resources/write", "schemas/write"));
        aclEntriesForMain.add(new ACLEntry(ACLEntry.Type.TYPE, "Authenticated", nexusClient.getDefaultRealm(), "resources/read", "projects/read"));
        for (String managerGroup : managerGroups) {
            aclEntriesForMain.add(new ACLEntry(ACLEntry.Type.GROUP, managerGroup, nexusClient.getDefaultRealm(), "projects/read", "resources/write"));
        }
        nexusClient.setAccessRightsForProject(spaceName, "main", aclEntriesForMain, credential);

        nexusClient.createProject(spaceName, "inferred", credential, "persistence layer for inferred instances (maintained by technical users only)", null);
        nexusClient.setAccessRightsForProject(spaceName, "inferred", Collections.singletonList(new ACLEntry(ACLEntry.Type.SUBJECT, serviceAccountSubject, oidcClient.getRealm(), "resources/write")), credential);

        nexusClient.createProject(spaceName, "release", credential, "add / remove / update releases", null);
        List<ACLEntry> aclEntriesForReleaseAndEditor =  new ArrayList<>();
        for (String managerGroup : managerGroups) {
            aclEntriesForReleaseAndEditor.add(new ACLEntry(ACLEntry.Type.GROUP, managerGroup, nexusClient.getDefaultRealm(), "projects/read", "resources/write", "resources/read"));
        }
        nexusClient.setAccessRightsForProject(spaceName, "release", aclEntriesForReleaseAndEditor, credential);

        nexusClient.createProject(spaceName, "editor", credential, "persistence layer for the editor", null);
        nexusClient.setAccessRightsForProject(spaceName, "editor", aclEntriesForReleaseAndEditor, credential);

        List<ACLEntry> aclEntriesForSuggest =  new ArrayList<>();
        nexusClient.createProject(spaceName, "suggest", credential, "persistence layer for suggestions (maintained by technical users only)", null);
        for (String managerGroup : managerGroups) {
            aclEntriesForSuggest.add(new ACLEntry(ACLEntry.Type.GROUP, managerGroup, nexusClient.getDefaultRealm(), "projects/read", "resources/read"));
        }
        nexusClient.setAccessRightsForProject(spaceName, "suggest", aclEntriesForSuggest, credential);
    }


    public void addServiceClientToPrivateSpace(String space, String client, List<String> resolvers){
        Credential credential = context.getCredential();
        Map<String, String> resolverPrefixes = new HashMap<>();
        for (String resolver : resolvers) {
            JsonDocument resolverDocFromDB = nexusClient.get(new NexusRelativeUrl(NexusConfiguration.ResourceType.PROJECTS, resolver), credential);
            if(resolverDocFromDB!=null){
                resolverPrefixes.put((String)resolverDocFromDB.get("_label"), (String)resolverDocFromDB.get("base"));
            }
        }
        nexusClient.createProject(space, client, credential, "a sub-space for a specific client", resolverPrefixes);
        String subject = "service-account-" + client;
        nexusClient.setAccessRightsForProject(space, client, Collections.singletonList(new ACLEntry(ACLEntry.Type.SUBJECT, subject, oidcClient.getRealm(),"resources/write", "resources/read", "projects/read")), credential);
        nexusClient.addAccessRightForOrganization(space, new ACLEntry(ACLEntry.Type.SUBJECT, subject, oidcClient.getRealm(), "organizations/read"), credential);
        nexusClient.setResolverForProject(space, client, resolvers, credential);
    }

}
