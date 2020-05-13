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

package org.humanbrainproject.knowledgegraph.instances.control;

import org.humanbrainproject.knowledgegraph.annotations.ToBeTested;
import org.humanbrainproject.knowledgegraph.commons.authorization.control.AuthorizationContext;
import org.humanbrainproject.knowledgegraph.commons.authorization.entity.Credential;
import org.humanbrainproject.knowledgegraph.commons.nexus.control.NexusClient;
import org.humanbrainproject.knowledgegraph.commons.nexus.control.NexusConfiguration;
import org.humanbrainproject.knowledgegraph.commons.vocabulary.NexusVocabulary;
import org.humanbrainproject.knowledgegraph.commons.vocabulary.SchemaOrgVocabulary;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusRelativeUrl;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusSchemaReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@ToBeTested(integrationTestRequired = true)
@Component
public class ContextController {

    @Autowired
    AuthorizationContext authorizationContext;

    @Autowired
    NexusClient nexusClient;

    public void createContext(NexusSchemaReference nexusContextReference, Map contextPayload, Credential credential) {
        Map context = nexusClient.get(nexusContextReference.getRelativeUrlForContext(), credential, Map.class);
        if (context == null) {
            if (nexusClient.get(nexusContextReference.getRelativeUrlForOrganization(), credential, Map.class) == null) {
                LinkedHashMap<String, String> payload = new LinkedHashMap<>();
                payload.put(SchemaOrgVocabulary.NAME, nexusContextReference.getOrganization());
                nexusClient.put(nexusContextReference.getRelativeUrlForOrganization(), null, payload, credential);
            }
            if (nexusClient.get(nexusContextReference.getRelativeUrlForDomain(), credential, Map.class) == null) {
                Map<String, String> payload = new LinkedHashMap<>();
                payload.put("description", String.format("The domain %s for organization %s", nexusContextReference.getDomain(), nexusContextReference.getOrganization()));
                nexusClient.put(nexusContextReference.getRelativeUrlForDomain(), null, payload, credential);
            }
            nexusClient.put(nexusContextReference.getRelativeUrlForContext(), null, contextPayload, credential);
            publishContext(nexusContextReference, 1, credential);
        } else {
            Boolean published = (Boolean) context.get(NexusVocabulary.PUBLISHED_ALIAS);
            if (!published) {
                Integer revision = (Integer) context.get(NexusVocabulary.REVISION_ALIAS);
                nexusClient.put(nexusContextReference.getRelativeUrlForContext(), revision, contextPayload, credential);
                publishContext(nexusContextReference, revision + 1, credential);
            }
        }
    }

    private void publishContext(NexusSchemaReference nexusSchemaReference, Integer revision, Credential credential) {
        Map<String, Boolean> payload = new LinkedHashMap<>();
        payload.put("published", true);
        nexusClient.patch(new NexusRelativeUrl(NexusConfiguration.ResourceType.CONTEXT, String.format("%s/config", nexusSchemaReference.getRelativeUrlForContext().getUrl())), revision, payload, credential);
    }
}
