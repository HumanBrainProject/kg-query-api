package org.humanbrainproject.knowledgegraph.instances.control;

import com.github.jsonldjava.core.JsonLdConsts;
import org.humanbrainproject.knowledgegraph.commons.authorization.entity.OidcAccessToken;
import org.humanbrainproject.knowledgegraph.commons.jsonld.control.JsonTransformer;
import org.humanbrainproject.knowledgegraph.commons.nexus.control.NexusClient;
import org.humanbrainproject.knowledgegraph.commons.nexus.control.NexusConfiguration;
import org.humanbrainproject.knowledgegraph.commons.nexus.control.SystemNexusClient;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.ArangoRepository;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoCollectionReference;
import org.humanbrainproject.knowledgegraph.commons.vocabulary.HBPVocabulary;
import org.humanbrainproject.knowledgegraph.commons.vocabulary.NexusVocabulary;
import org.humanbrainproject.knowledgegraph.commons.vocabulary.SchemaOrgVocabulary;
import org.humanbrainproject.knowledgegraph.indexing.boundary.GraphIndexing;
import org.humanbrainproject.knowledgegraph.indexing.entity.IndexingMessage;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusInstanceReference;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusRelativeUrl;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusSchemaReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

@Component
public class InstanceController {

    @Autowired
    SystemNexusClient systemNexusClient;

    @Autowired
    NexusClient nexusClient;

    @Autowired
    SchemaController schemaController;

    @Autowired
    GraphIndexing graphIndexing;

    @Autowired
    JsonTransformer jsonTransformer;

    @Autowired
    ArangoRepository arangoRepository;


    private NexusInstanceReference getByIdentifier(NexusSchemaReference schema, String identifier) {
        return arangoRepository.findBySchemaOrgIdentifier(ArangoCollectionReference.fromNexusSchemaReference(schema), identifier);
    }

    public NexusInstanceReference createInstanceByIdentifier(NexusSchemaReference schemaReference, String identifier, Map<String, Object> payload, OidcAccessToken oidcAccessToken) {
        Object o = payload.get(SchemaOrgVocabulary.IDENTIFIER);
        if(o==null){
            payload.put(SchemaOrgVocabulary.IDENTIFIER, identifier);
        }
        else if(o instanceof Collection && !((Collection)o).contains(identifier)){
            ((Collection)o).add(identifier);
        }
        else if(!o.equals(identifier)){
            payload.put(SchemaOrgVocabulary.IDENTIFIER, Arrays.asList(o, identifier));
        }
        NexusInstanceReference byIdentifier = getByIdentifier(schemaReference, identifier);
        if (byIdentifier==null) {
            return createInstanceByNexusId(schemaReference, null, null, payload, oidcAccessToken);
        } else {
            return createInstanceByNexusId(byIdentifier.getNexusSchema(), byIdentifier.getId(), byIdentifier.getRevision(), payload, oidcAccessToken);
        }
    }

    public void deprecateInstanceByNexusId(NexusInstanceReference instanceReference, OidcAccessToken oidcAccessToken){
        boolean delete = nexusClient.delete(instanceReference.getRelativeUrl(), instanceReference.getRevision() != null ? instanceReference.getRevision() : 1, oidcAccessToken);
        if(delete){
            immediateDeprecation(instanceReference);
        }
    }

    public NexusInstanceReference createInstanceByNexusId(NexusSchemaReference nexusSchemaReference, String id, Integer revision, Map<String, Object> payload, OidcAccessToken oidcAccessToken)  {
        schemaController.createSchema(nexusSchemaReference);
        Object type = payload.get(JsonLdConsts.TYPE);
        String targetClass = schemaController.getTargetClass(nexusSchemaReference);
        if (type == null) {
            payload.put(JsonLdConsts.TYPE, targetClass);
        } else if (type instanceof Collection) {
            if(!((Collection)type).contains(targetClass)) {
                ((Collection)type).add(targetClass);
            }
        } else if (!type.equals(targetClass)) {
            payload.put(JsonLdConsts.TYPE, Arrays.asList(type, targetClass));
        }
        NexusInstanceReference nexusInstanceReference = new NexusInstanceReference(nexusSchemaReference, id);
        NexusInstanceReference newInstanceReference = null;
        if (revision == null) {
            Map map = systemNexusClient.get(nexusInstanceReference.getRelativeUrl());
            if (map == null) {
                Map post = nexusClient.post(new NexusRelativeUrl(NexusConfiguration.ResourceType.DATA, nexusInstanceReference.getNexusSchema().getRelativeUrl().getUrl()), null, payload, oidcAccessToken);
                if (post != null && post.containsKey(JsonLdConsts.ID)) {
                    NexusInstanceReference fromUrl = NexusInstanceReference.createFromUrl((String) post.get(JsonLdConsts.ID));
                    fromUrl.setRevision((Integer) post.get(NexusVocabulary.REVISION_ALIAS));
                    newInstanceReference = fromUrl;
                }
                else {
                    newInstanceReference = null;
                }
            } else {
                Map put = nexusClient.put(nexusInstanceReference.getRelativeUrl(), (Integer) map.get(NexusVocabulary.REVISION_ALIAS), payload, oidcAccessToken);
                if (put.containsKey(NexusVocabulary.REVISION_ALIAS)) {
                    nexusInstanceReference.setRevision((Integer) put.get(NexusVocabulary.REVISION_ALIAS));
                }
                newInstanceReference = nexusInstanceReference;
            }
        } else {
            Map put = nexusClient.put(nexusInstanceReference.getRelativeUrl(), revision, payload, oidcAccessToken);
            if (put.containsKey(NexusVocabulary.REVISION_ALIAS)) {
                nexusInstanceReference.setRevision((Integer) put.get(NexusVocabulary.REVISION_ALIAS));
            }
            newInstanceReference = nexusInstanceReference;
        }
        if(newInstanceReference!=null) {
            immediateIndexing(payload, newInstanceReference);
        }
        return newInstanceReference;
    }

    private void immediateDeprecation(NexusInstanceReference newInstanceReference) {
        graphIndexing.delete(newInstanceReference);
    }

    private void immediateIndexing(Map<String, Object> payload, NexusInstanceReference newInstanceReference) {
        payload.put(HBPVocabulary.PROVENANCE_IMMEDIATE_INDEX, true);
        IndexingMessage indexingMessage = new IndexingMessage(newInstanceReference, jsonTransformer.getMapAsJson(payload), null, null);
        graphIndexing.insert(indexingMessage);
    }

}
