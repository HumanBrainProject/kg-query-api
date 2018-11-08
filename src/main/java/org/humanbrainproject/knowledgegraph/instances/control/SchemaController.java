package org.humanbrainproject.knowledgegraph.instances.control;

import com.github.jsonldjava.core.JsonLdConsts;
import com.google.gson.Gson;
import org.apache.commons.lang3.StringUtils;
import org.humanbrainproject.knowledgegraph.commons.authorization.entity.OidcAccessToken;
import org.humanbrainproject.knowledgegraph.commons.nexus.control.NexusClient;
import org.humanbrainproject.knowledgegraph.commons.nexus.control.NexusConfiguration;
import org.humanbrainproject.knowledgegraph.commons.nexus.control.SystemNexusClient;
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

@Component
public class SchemaController {

    @Autowired
    NexusClient nexusClient;

    @Autowired
    SystemNexusClient systemNexusClient;

    @Value("classpath:nexusExt/defaultContext.json")
    private Resource defaultContext;

    private Map defaultContextPayload;

    private Map getDefaultContextPayload() {
        if (defaultContextPayload == null) {
            try (Reader reader = new BufferedReader(new InputStreamReader(defaultContext.getInputStream()));) {
                this.defaultContextPayload = new Gson().fromJson(reader, Map.class);
            }
            catch (IOException e){
                throw new RuntimeException("Can not find default context!");
            }
        }
        return this.defaultContextPayload;
    }


    public void createSchema(NexusSchemaReference nexusSchemaReference, Map schemaPayload) {
        Map schema = systemNexusClient.get(nexusSchemaReference.getRelativeUrl());
        if (schema == null) {
            if (systemNexusClient.get(nexusSchemaReference.getRelativeUrlForOrganization()) == null) {
                systemNexusClient.put(nexusSchemaReference.getRelativeUrlForOrganization(), null, new LinkedHashMap());
            }
            if (systemNexusClient.get(nexusSchemaReference.getRelativeUrlForDomain()) == null) {
                Map<String, String> payload = new LinkedHashMap<>();
                payload.put("description", String.format("The domain %s for organization %s", nexusSchemaReference.getDomain(), nexusSchemaReference.getOrganization()));
                systemNexusClient.put(nexusSchemaReference.getRelativeUrlForDomain(), null, payload);
            }
            systemNexusClient.put(nexusSchemaReference.getRelativeUrl(), null, schemaPayload);
            publishSchema(nexusSchemaReference, 1);
        } else {
            Boolean published = (Boolean) schema.get(NexusVocabulary.PUBLISHED_ALIAS);
            if (!published) {
                Integer revision = (Integer) schema.get(NexusVocabulary.REVISION_ALIAS);
                systemNexusClient.put(nexusSchemaReference.getRelativeUrl(), revision, schemaPayload);
                publishSchema(nexusSchemaReference, revision + 1);
            }
        }
    }


    private void publishSchema(NexusSchemaReference nexusSchemaReference, Integer revision) {
        Map<String, Boolean> payload = new LinkedHashMap<>();
        payload.put("published", true);
        systemNexusClient.patch(new NexusRelativeUrl(NexusConfiguration.ResourceType.SCHEMA, String.format("%s/config", nexusSchemaReference.getRelativeUrl().getUrl())), revision, payload);
    }

    public void createSchema(NexusSchemaReference nexusSchemaReference) {
        createSchema(nexusSchemaReference, createSimpleSchema(nexusSchemaReference));
    }

    String getOrganization(NexusSchemaReference schemaReference) {
        return String.format("%s%s/", HBPVocabulary.NAMESPACE, schemaReference.getOrganization());
    }

    String getTargetClass(NexusSchemaReference schemaReference){
        return String.format("%s%s", getOrganization(schemaReference), StringUtils.capitalize(schemaReference.getSchema()));
    }


    private Map createSimpleSchema(NexusSchemaReference schemaReference) {
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

    public void clearAllInstancesFromSchema(NexusSchemaReference schema, OidcAccessToken oidcAccessToken){
        List<JsonDocument> documents = nexusClient.list(schema, oidcAccessToken, true);
        for (JsonDocument document : documents) {
            NexusInstanceReference instanceReference = NexusInstanceReference.createFromUrl((String)document.get("resultId"));
            JsonDocument doc = nexusClient.get(instanceReference.getRelativeUrl(), oidcAccessToken);
            nexusClient.delete(instanceReference.getRelativeUrl(), doc.getNexusRevision(), oidcAccessToken);
        }
    }

    public List<NexusSchemaReference> getAllSchemas(String organization, OidcAccessToken oidcAccessToken){
        return nexusClient.listSchemasByOrganization(organization, oidcAccessToken, true);
    }



}
