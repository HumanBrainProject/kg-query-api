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

package org.humanbrainproject.knowledgegraph.instances.control;

import com.github.jsonldjava.core.JsonLdConsts;
import com.google.gson.Gson;
import org.apache.commons.lang3.StringUtils;
import org.humanbrainproject.knowledgegraph.annotations.ToBeTested;
import org.humanbrainproject.knowledgegraph.commons.authorization.control.AuthorizationContext;
import org.humanbrainproject.knowledgegraph.commons.authorization.entity.Credential;
import org.humanbrainproject.knowledgegraph.commons.authorization.entity.InternalMasterKey;
import org.humanbrainproject.knowledgegraph.commons.nexus.control.NexusClient;
import org.humanbrainproject.knowledgegraph.commons.nexus.control.NexusConfiguration;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.entity.SubSpace;
import org.humanbrainproject.knowledgegraph.commons.vocabulary.*;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusInstanceReference;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusRelativeUrl;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusSchemaReference;
import org.humanbrainproject.knowledgegraph.query.entity.JsonDocument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@ToBeTested(integrationTestRequired = true)
@Component
public class SchemaController {

    @Autowired
    AuthorizationContext authorizationContext;

    @Autowired
    NexusClient nexusClient;

    @Value("classpath:nexusExt/defaultContext.json")
    private Resource defaultContext;

    private Map defaultContextPayload;

    private Map getDefaultContextPayload() {
        if (defaultContextPayload == null) {
            try (Reader reader = new BufferedReader(new InputStreamReader(defaultContext.getInputStream()));) {
                this.defaultContextPayload = new Gson().fromJson(reader, Map.class);
            } catch (IOException e) {
                throw new RuntimeException("Can not find default context!");
            }
        }
        return this.defaultContextPayload;
    }


    public void createSchema(NexusSchemaReference nexusSchemaReference, Map schemaPayload, Credential credential) {
        Map schema = nexusClient.get(nexusSchemaReference.getRelativeUrl(), credential, Map.class);
        if (schema == null) {
            if (nexusClient.get(nexusSchemaReference.getRelativeUrlForOrganization(), credential, Map.class) == null) {
                LinkedHashMap<String, String> payload = new LinkedHashMap<>();
                payload.put(SchemaOrgVocabulary.NAME, nexusSchemaReference.getOrganization());
                nexusClient.put(nexusSchemaReference.getRelativeUrlForOrganization(), null, payload, credential);
            }
            if (nexusClient.get(nexusSchemaReference.getRelativeUrlForDomain(), credential, Map.class) == null) {
                Map<String, String> payload = new LinkedHashMap<>();
                payload.put("description", String.format("The domain %s for organization %s", nexusSchemaReference.getDomain(), nexusSchemaReference.getOrganization()));
                nexusClient.put(nexusSchemaReference.getRelativeUrlForDomain(), null, payload, credential);
            }
            nexusClient.put(nexusSchemaReference.getRelativeUrl(), null, schemaPayload, credential);
            publishSchema(nexusSchemaReference, 1, credential);
        } else {
            Boolean published = (Boolean) schema.get(NexusVocabulary.PUBLISHED_ALIAS);
            if (!published) {
                Integer revision = (Integer) schema.get(NexusVocabulary.REVISION_ALIAS);
                nexusClient.put(nexusSchemaReference.getRelativeUrl(), revision, schemaPayload, credential);
                publishSchema(nexusSchemaReference, revision + 1, credential);
            }
        }
    }


    private void publishSchema(NexusSchemaReference nexusSchemaReference, Integer revision, Credential credential) {
        Map<String, Boolean> payload = new LinkedHashMap<>();
        payload.put("published", true);
        nexusClient.patch(new NexusRelativeUrl(NexusConfiguration.ResourceType.SCHEMA, String.format("%s/config", nexusSchemaReference.getRelativeUrl().getUrl())), revision, payload, credential);
    }



    public void createSchema(NexusSchemaReference nexusSchemaReference) {
        createSchema(nexusSchemaReference, createSimpleSchema(nexusSchemaReference), nexusSchemaReference.isInSubSpace(SubSpace.INFERRED) ? new InternalMasterKey() : authorizationContext.getCredential());
        if(!nexusSchemaReference.isInSubSpace(SubSpace.INFERRED)) {
            NexusSchemaReference inferredSchema = nexusSchemaReference.toSubSpace(SubSpace.INFERRED);
            createSchema(inferredSchema, createSimpleSchema(inferredSchema), new InternalMasterKey());
        }

    }

    String getOrganization(NexusSchemaReference schemaReference) {
        return String.format("%s%s/", HBPVocabulary.NAMESPACE, schemaReference.getOrganization());
    }

    public String getTargetClass(NexusSchemaReference schemaReference) {
        return String.format("%s%s", getOrganization(schemaReference), StringUtils.capitalize(schemaReference.getSchema()));
    }


    public Map createSimpleSchema(NexusSchemaReference schemaReference) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put(JsonLdConsts.CONTEXT, getDefaultContextPayload());
        Map<String, Object> shape = new LinkedHashMap<>();
        String organization = getOrganization(schemaReference);
        shape.put(JsonLdConsts.ID, String.format("%s%sShape", organization, StringUtils.capitalize(schemaReference.getSchema())));
        shape.put(JsonLdConsts.TYPE, ShaclVocabulary.NODE_SHAPE_ALIAS);
        Map<String, String> identifierProperty = new LinkedHashMap<>();
        identifierProperty.put("datatype", XSDVocabulary.STRING_ALIAS);
        identifierProperty.put("path", SchemaOrgVocabulary.IDENTIFIER);
        shape.put("property", Collections.singletonList(identifierProperty));
        shape.put("targetClass", getTargetClass(schemaReference));
        schema.put("shapes", Collections.singletonList(shape));
        return schema;

    }

    public void clearAllInstancesFromSchema(NexusSchemaReference schema) {
        List<JsonDocument> documents = nexusClient.list(schema, authorizationContext.getCredential(), true);
        for (JsonDocument document : documents) {
            NexusInstanceReference instanceReference = NexusInstanceReference.createFromUrl((String) document.get("resultId"));
            JsonDocument doc = nexusClient.get(instanceReference.getRelativeUrl(), authorizationContext.getCredential());
            nexusClient.delete(instanceReference.getRelativeUrl(), doc.getNexusRevision(), authorizationContext.getCredential());
        }
    }

    public List<NexusSchemaReference> getAllSchemas(String organization) {
        return nexusClient.listSchemasByOrganization(organization, authorizationContext.getCredential(), true);
    }


}
