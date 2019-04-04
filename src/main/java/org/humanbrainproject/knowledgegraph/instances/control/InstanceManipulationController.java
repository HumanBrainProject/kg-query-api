package org.humanbrainproject.knowledgegraph.instances.control;

import com.github.jsonldjava.core.JsonLdConsts;
import org.humanbrainproject.knowledgegraph.annotations.ToBeTested;
import org.humanbrainproject.knowledgegraph.commons.authorization.control.AuthorizationContext;
import org.humanbrainproject.knowledgegraph.commons.authorization.control.AuthorizationController;
import org.humanbrainproject.knowledgegraph.commons.authorization.entity.Credential;
import org.humanbrainproject.knowledgegraph.commons.authorization.entity.InternalMasterKey;
import org.humanbrainproject.knowledgegraph.commons.jsonld.control.JsonTransformer;
import org.humanbrainproject.knowledgegraph.commons.nexus.control.NexusClient;
import org.humanbrainproject.knowledgegraph.commons.nexus.control.NexusConfiguration;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.ArangoNativeRepository;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoDocumentReference;
import org.humanbrainproject.knowledgegraph.commons.vocabulary.HBPVocabulary;
import org.humanbrainproject.knowledgegraph.commons.vocabulary.NexusVocabulary;
import org.humanbrainproject.knowledgegraph.commons.vocabulary.SchemaOrgVocabulary;
import org.humanbrainproject.knowledgegraph.indexing.boundary.GraphIndexing;
import org.humanbrainproject.knowledgegraph.indexing.entity.IndexingMessage;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusInstanceReference;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusRelativeUrl;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusSchemaReference;
import org.humanbrainproject.knowledgegraph.query.entity.JsonDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

@ToBeTested(integrationTestRequired = true)
@Component
public class InstanceManipulationController {

    @Autowired
    AuthorizationController authorizationController;

    @Autowired
    InstanceLookupController lookupController;

    @Autowired
    AuthorizationContext authorizationContext;

    @Autowired
    NexusClient nexusClient;

    @Autowired
    SchemaController schemaController;

    @Autowired
    GraphIndexing graphIndexing;

    @Autowired
    JsonTransformer jsonTransformer;

    @Autowired
    ArangoNativeRepository arangoNativeRepository;

    @Autowired
    NexusConfiguration nexusConfiguration;


    private Logger logger = LoggerFactory.getLogger(InstanceManipulationController.class);


    public NexusInstanceReference updateInstance(NexusInstanceReference instanceReference, Map<String, Object> payload, String clientIdExtension) {
        NexusInstanceReference originalId = arangoNativeRepository.findOriginalId(instanceReference);
        try{
            Object indexedAt = payload.get(HBPVocabulary.PROVENANCE_MODIFIED_AT);
            LocalDateTime.parse((String) indexedAt, DateTimeFormatter.ISO_ZONED_DATE_TIME);
        } catch (DateTimeParseException d)  {
            throw new RuntimeException(String.format("Could not parse update timestamp for instance: %s", originalId.toString() ));
        }
        JsonDocument instance = arangoNativeRepository.getInstance(ArangoDocumentReference.fromNexusInstance(originalId));
        if (instance == null) {
            return null;
        }
        NexusSchemaReference nexusSchema = instanceReference.getNexusSchema();
        nexusSchema = nexusSchema.toSubSpace(authorizationContext.getSubspace());
        JsonDocument document = new JsonDocument(payload);
        String primaryIdentifier = instance.getPrimaryIdentifier();
        if (primaryIdentifier == null) {
            if (clientIdExtension == null && authorizationContext.getSubspace() == originalId.getSubspace()) {
                //It's a replacement of the original instance - we therefore should be able to insert the data even without identifier mapping
                logger.warn(String.format("Found instance without identifier: %s - since it was an update for the original resource, I can continue - but please check what is happening here!", instanceReference.getRelativeUrl().getUrl()));
                return createInstanceByNexusId(nexusSchema, originalId.getId(), instanceReference.getRevision() != null ? instanceReference.getRevision() : 1, document, clientIdExtension);
            } else {
                throw new RuntimeException(String.format("Found instance without identifier: %s", instanceReference.getRelativeUrl().getUrl()));
            }
        }
        if (clientIdExtension != null || (authorizationContext.getSubspace() != originalId.getSubspace())) {
            document.addReference(HBPVocabulary.INFERENCE_EXTENDS, nexusConfiguration.getAbsoluteUrl(originalId));
        }
        primaryIdentifier = lookupController.constructIdentifierWithClientIdExtension(primaryIdentifier, clientIdExtension);
        return createInstanceByIdentifier(nexusSchema, primaryIdentifier, document, clientIdExtension);
    }




    public NexusInstanceReference createInstanceByIdentifier(NexusSchemaReference schemaReference, String identifier, JsonDocument payload, String userId) {
        payload.addToProperty(SchemaOrgVocabulary.IDENTIFIER, identifier);
        NexusInstanceReference byIdentifier = lookupController.getByIdentifier(schemaReference, identifier);
        if (byIdentifier == null) {
            return createInstanceByNexusId(schemaReference, null, 1, payload, userId);
        } else {
            return createInstanceByNexusId(byIdentifier.getNexusSchema(), byIdentifier.getId(), byIdentifier.getRevision(), payload, userId);
        }
    }

    public boolean deprecateInstanceByNexusId(NexusInstanceReference instanceReference) {
        boolean delete = nexusClient.delete(instanceReference.getRelativeUrl(), instanceReference.getRevision() != null ? instanceReference.getRevision() : 1, authorizationContext.getCredential());
        if (delete) {
            immediateDeprecation(instanceReference);
        }
        return delete;
    }

    /**
     * Creates a new instance with the given payload. Also creates the organization, domain and schema if not yet available.
     */
    public NexusInstanceReference createNewInstance(NexusSchemaReference nexusSchemaReference, Map<String, Object> originalPayload, String clientIdExtension) {
        nexusSchemaReference = nexusSchemaReference.toSubSpace(authorizationContext.getSubspace());
        schemaController.createSchema(nexusSchemaReference);
        JsonDocument payload;
        if (originalPayload != null) {
            payload = new JsonDocument(originalPayload);
        } else {
            payload = new JsonDocument();
        }
        payload.addType(schemaController.getTargetClass(nexusSchemaReference));
        payload.addToProperty(SchemaOrgVocabulary.IDENTIFIER, "");
        payload.addToProperty(HBPVocabulary.PROVENANCE_CREATED_AT, ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT));
        payload.addToProperty(HBPVocabulary.PROVENANCE_CREATED_BY, clientIdExtension);
        JsonDocument response = nexusClient.post(new NexusRelativeUrl(NexusConfiguration.ResourceType.RESOURCES, nexusSchemaReference.getRelativeUrl().getUrl()), null, payload, authorizationContext.getCredential());
        if (response != null) {
            NexusInstanceReference idFromNexus = response.getReference();
            //We're replacing the previously set identifier with the id we got from Nexus.
            payload.put(SchemaOrgVocabulary.IDENTIFIER, idFromNexus.getId());
            JsonDocument result = nexusClient.put(idFromNexus.getRelativeUrl(), idFromNexus.getRevision(), payload, authorizationContext.getCredential());
            NexusInstanceReference fromUpdate = NexusInstanceReference.createFromUrl((String) result.get(JsonLdConsts.ID));
            Object rev = result.get(NexusVocabulary.REVISION_ALIAS);
            if (rev != null) {
                fromUpdate.setRevision(Integer.valueOf(rev.toString()));
            }

            immediateIndexing(payload, fromUpdate, clientIdExtension);
            return fromUpdate;
        }
        return null;
    }

    public NexusInstanceReference  createInstanceByNexusId(NexusSchemaReference nexusSchemaReference, String id, Integer revision, Map<String, Object> payload, String userId){
        return createInstanceByNexusId(nexusSchemaReference, id, revision, payload, userId, null);
    }

    /**
     * ATTENTION! This method acts as the system-user with extended rights. Make sure it is only used if the content is under our control!
     */
    public NexusInstanceReference createInstanceByNexusIdAsSystemUser(NexusSchemaReference nexusSchemaReference, String id, Integer revision, Map<String, Object> payload, String userId){
        return createInstanceByNexusId(nexusSchemaReference, id, revision, payload, userId, new InternalMasterKey());
    }


    private NexusInstanceReference createInstanceByNexusId(NexusSchemaReference nexusSchemaReference, String id, Integer revision, Map<String, Object> payload, String userId, Credential credential) {
        if(credential==null){
            credential = authorizationContext.getCredential();
        }
        ClientHttpRequestInterceptor interceptor = authorizationController.getInterceptor(credential);

        schemaController.createSchema(nexusSchemaReference);
        Object type = payload.get(JsonLdConsts.TYPE);
        String targetClass = schemaController.getTargetClass(nexusSchemaReference);
        if (type == null) {
            payload.put(JsonLdConsts.TYPE, targetClass);
        } else if (type instanceof Collection) {
            if (!((Collection) type).contains(targetClass)) {
                ((Collection) type).add(targetClass);
            }
        } else if (!type.equals(targetClass)) {
            payload.put(JsonLdConsts.TYPE, Arrays.asList(type, targetClass));
        }
        NexusInstanceReference nexusInstanceReference = new NexusInstanceReference(nexusSchemaReference, id).setRevision(revision);
        NexusInstanceReference newInstanceReference = null;
        payload.put(HBPVocabulary.PROVENANCE_MODIFIED_AT, ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT));
        if(userId != null){
            payload.put(HBPVocabulary.PROVENANCE_LAST_MODIFICATION_USER_ID, userId);
        }
        if (revision == null || revision == 1) {
            Map map = nexusClient.get(nexusInstanceReference.getRelativeUrl(), credential);

            if (map == null) {
                Map post = nexusClient.post(new NexusRelativeUrl(NexusConfiguration.ResourceType.RESOURCES, nexusInstanceReference.getNexusSchema().getRelativeUrl().getUrl()), null, payload, interceptor);
                if (post != null && post.containsKey(JsonLdConsts.ID)) {
                    NexusInstanceReference fromUrl = NexusInstanceReference.createFromUrl((String) post.get(JsonLdConsts.ID));
                    fromUrl.setRevision((Integer) post.get(NexusVocabulary.REVISION_ALIAS));
                    newInstanceReference = fromUrl;
                } else {
                    newInstanceReference = null;
                }
            } else {
                Map put = nexusClient.put(nexusInstanceReference.getRelativeUrl(), (Integer) map.get(NexusVocabulary.REVISION_ALIAS), payload, interceptor);
                if (put.containsKey(NexusVocabulary.REVISION_ALIAS)) {
                    nexusInstanceReference.setRevision((Integer) put.get(NexusVocabulary.REVISION_ALIAS));
                }
                newInstanceReference = nexusInstanceReference;
            }
        } else {
            Map put = nexusClient.put(nexusInstanceReference.getRelativeUrl(), revision, payload, interceptor);
            if (put.containsKey(NexusVocabulary.REVISION_ALIAS)) {
                nexusInstanceReference.setRevision((Integer) put.get(NexusVocabulary.REVISION_ALIAS));
            }
            newInstanceReference = nexusInstanceReference;
        }
        if (newInstanceReference != null) {
            immediateIndexing(payload, newInstanceReference, userId);
        }
        return newInstanceReference;
    }

    private void immediateDeprecation(NexusInstanceReference newInstanceReference) {
        graphIndexing.delete(newInstanceReference);
    }

    private void immediateIndexing(Map<String, Object> payload, NexusInstanceReference newInstanceReference, String userId) {
        payload.put(HBPVocabulary.PROVENANCE_IMMEDIATE_INDEX, true);
        IndexingMessage indexingMessage = new IndexingMessage(newInstanceReference, jsonTransformer.getMapAsJson(payload), null, userId);
        graphIndexing.insert(indexingMessage);
    }

    public boolean removeInstance(NexusInstanceReference nexusInstanceReference) {
        //We only deprecate the original id - this way, the reconciled instance should disappear.
        return deprecateInstanceByNexusId(arangoNativeRepository.findOriginalId(nexusInstanceReference));
    }

    public JsonDocument directInstanceUpdate(NexusInstanceReference ref, Integer revision, JsonDocument payload,  Credential credential){
        if(credential==null){
            credential = authorizationContext.getCredential();
        }
        ClientHttpRequestInterceptor interceptor = authorizationController.getInterceptor(credential);
        return nexusClient.put(ref.getRelativeUrl(), revision, payload, interceptor);
    }


}
