package org.humanbrainproject.knowledgegraph.instances.control;

import org.humanbrainproject.knowledgegraph.annotations.ToBeTested;
import org.humanbrainproject.knowledgegraph.commons.authorization.control.AuthorizationContext;
import org.humanbrainproject.knowledgegraph.commons.nexus.control.NexusClient;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.ArangoNativeRepository;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.ArangoRepository;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoCollectionReference;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoDocumentReference;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.entity.SubSpace;
import org.humanbrainproject.knowledgegraph.commons.vocabulary.NexusVocabulary;
import org.humanbrainproject.knowledgegraph.context.QueryContext;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusInstanceReference;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusSchemaReference;
import org.humanbrainproject.knowledgegraph.query.entity.DatabaseScope;
import org.humanbrainproject.knowledgegraph.query.entity.JsonDocument;
import org.humanbrainproject.knowledgegraph.query.entity.Pagination;
import org.humanbrainproject.knowledgegraph.query.entity.QueryResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@ToBeTested(integrationTestRequired = true)
public class InstanceLookupController {

    @Autowired
    AuthorizationContext authorizationContext;

    @Autowired
    QueryContext queryContext;

    @Autowired
    NexusClient nexusClient;

    @Autowired
    ArangoRepository arangoRepository;

    @Autowired
    ArangoNativeRepository arangoNativeRepository;


    private Logger logger = LoggerFactory.getLogger(InstanceLookupController.class);

    public List<Map> getLinkingInstances(NexusInstanceReference fromInstance, NexusInstanceReference toInstance, NexusSchemaReference relationType) {
        if (fromInstance == null || toInstance == null || relationType == null) {
            return null;
        }
        return arangoRepository.getLinkingInstances(ArangoDocumentReference.fromNexusInstance(fromInstance), ArangoDocumentReference.fromNexusInstance(toInstance), ArangoCollectionReference.fromNexusSchemaReference(relationType), queryContext.getDatabaseConnection());
    }


    public JsonDocument getInstanceByClientExtension(NexusInstanceReference instanceReference, String clientExtension) {
        NexusSchemaReference schemaReference = instanceReference.getNexusSchema().toSubSpace(authorizationContext.getSubspace());
        NexusInstanceReference originalId = arangoNativeRepository.findOriginalId(instanceReference);
        JsonDocument instance = arangoNativeRepository.getInstance(ArangoDocumentReference.fromNexusInstance(originalId));
        if (instance != null) {
            String identifier = constructIdentifierWithClientIdExtension(instance.getPrimaryIdentifier(), clientExtension);
            NexusInstanceReference bySchemaOrgIdentifier = arangoNativeRepository.findBySchemaOrgIdentifier(ArangoCollectionReference.fromNexusSchemaReference(schemaReference), identifier);
            if (bySchemaOrgIdentifier != null) {
                return new JsonDocument(arangoNativeRepository.getDocument(ArangoDocumentReference.fromNexusInstance(bySchemaOrgIdentifier))).removeAllInternalKeys();
            }
            return null;
        }
        return null;
    }


    String constructIdentifierWithClientIdExtension(String identifier, String clientIdExtension) {
        return clientIdExtension != null ? identifier + clientIdExtension : identifier;

    }

    NexusInstanceReference getByIdentifier(NexusSchemaReference schema, String identifier) {
        NexusInstanceReference bySchemaOrgIdentifier = arangoNativeRepository.findBySchemaOrgIdentifier(ArangoCollectionReference.fromNexusSchemaReference(schema), identifier);
        if (bySchemaOrgIdentifier != null) {
            JsonDocument fromNexusById = getFromNexusById(bySchemaOrgIdentifier);
            Object revision = fromNexusById.get(NexusVocabulary.REVISION_ALIAS);
            if (revision != null) {
                bySchemaOrgIdentifier.setRevision(Integer.valueOf(revision.toString()));
            }
        }
        return bySchemaOrgIdentifier;
    }

    /**
     * Returns the original JSON payload from Nexus
     */
    JsonDocument getFromNexusById(NexusInstanceReference instanceReference) {
        return nexusClient.get(instanceReference.getRelativeUrl(), authorizationContext.getCredential());
    }


    List<NexusInstanceReference> getAllInstancesForSchema(NexusSchemaReference nexusSchemaReference) {
        List<JsonDocument> list = nexusClient.list(nexusSchemaReference, authorizationContext.getCredential(), true);
        if (list != null) {
            return list.stream().map(d -> d.get("resultId")).filter(o -> o instanceof String).map(o -> NexusInstanceReference.createFromUrl((String) o)).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    public JsonDocument getInstance(NexusInstanceReference instanceReference) {
        if (instanceReference == null) {
            return null;
        }
        NexusInstanceReference lookupId = instanceReference;
        if (queryContext.getDatabaseScope() != DatabaseScope.NATIVE) {
            lookupId = arangoNativeRepository.findOriginalId(instanceReference);
            if (lookupId == null) {
                return null;
            }
            lookupId = instanceReference.toSubSpace(SubSpace.MAIN);
        }
        JsonDocument instance = arangoRepository.getInstance(ArangoDocumentReference.fromNexusInstance(lookupId), queryContext.getDatabaseConnection());
        return instance != null ? instance.removeAllInternalKeys() : null;
    }

    public QueryResult<List<Map>> getInstances(NexusSchemaReference schemaReference, String searchTerm, Pagination pagination) {
        return arangoRepository.getInstances(ArangoCollectionReference.fromNexusSchemaReference(schemaReference), pagination!=null ? pagination.getStart() : null, pagination!=null ? pagination.getSize() : null, searchTerm, queryContext.getDatabaseConnection());
    }

    public JsonDocument findInstanceByIdentifier(NexusSchemaReference schema, String identifier) {
        NexusInstanceReference reference = arangoNativeRepository.findBySchemaOrgIdentifier(ArangoCollectionReference.fromNexusSchemaReference(schema), identifier);
        if (reference != null) {
            NexusInstanceReference originalId = arangoNativeRepository.findOriginalId(reference);
            if (originalId != null) {
                return getInstance(originalId.toSubSpace(SubSpace.MAIN)).removeAllInternalKeys();
            }
        }
        return null;
    }

}
