package org.humanbrainproject.knowledgegraph.instances.boundary;

import org.humanbrainproject.knowledgegraph.commons.authorization.entity.Credential;
import org.humanbrainproject.knowledgegraph.commons.jsonld.control.JsonLdStandardization;
import org.humanbrainproject.knowledgegraph.commons.jsonld.control.JsonTransformer;
import org.humanbrainproject.knowledgegraph.commons.nexus.control.NexusClient;
import org.humanbrainproject.knowledgegraph.commons.nexus.control.NexusConfiguration;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.ArangoConnection;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.ArangoDatabaseFactory;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.ArangoRepository;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoCollectionReference;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoDocumentReference;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.entity.SubSpace;
import org.humanbrainproject.knowledgegraph.commons.vocabulary.HBPVocabulary;
import org.humanbrainproject.knowledgegraph.indexing.boundary.GraphIndexing;
import org.humanbrainproject.knowledgegraph.indexing.control.MessageProcessor;
import org.humanbrainproject.knowledgegraph.indexing.entity.IndexingMessage;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusInstanceReference;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusSchemaReference;
import org.humanbrainproject.knowledgegraph.instances.control.InstanceController;
import org.humanbrainproject.knowledgegraph.instances.control.SchemaController;
import org.humanbrainproject.knowledgegraph.instances.entity.Client;
import org.humanbrainproject.knowledgegraph.query.entity.DatabaseScope;
import org.humanbrainproject.knowledgegraph.query.entity.JsonDocument;
import org.humanbrainproject.knowledgegraph.query.entity.QueryParameters;
import org.humanbrainproject.knowledgegraph.query.entity.QueryResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Component
public class Instances {


    @Autowired
    ArangoRepository arangoRepository;

    @Autowired
    ArangoDatabaseFactory databaseFactory;

    @Autowired
    InstanceController instanceController;

    @Autowired
    MessageProcessor messageProcessor;

    @Autowired
    JsonLdStandardization jsonLdStandardization;

    @Autowired
    JsonTransformer jsonTransformer;

    @Autowired
    NexusConfiguration nexusConfiguration;

    @Autowired
    SchemaController schemaController;

    @Autowired
    GraphIndexing graphIndexing;

    @Autowired
    NexusClient nexusClient;


    private Logger logger = LoggerFactory.getLogger(Instances.class);


    public JsonDocument getInstance(NexusInstanceReference instanceReference, DatabaseScope databaseScope, Credential credential) {
        if(instanceReference==null){
            return null;
        }
        NexusInstanceReference lookupId;
        if(databaseScope!=DatabaseScope.NATIVE) {
            lookupId = arangoRepository.findOriginalId(instanceReference, credential);
            if (lookupId == null) {
                return null;
            }
        }
        else {
            lookupId = instanceReference.toSubSpace(SubSpace.MAIN);
        }

        return getInstance(lookupId, databaseFactory.getConnection(databaseScope), credential).removeAllInternalKeys();
    }

    public QueryResult<List<Map>> getInstances(NexusSchemaReference schemaReference, QueryParameters queryParameters, Credential credential) {
        return arangoRepository.getInstances(ArangoCollectionReference.fromNexusSchemaReference(schemaReference), queryParameters.pagination().getStart(), queryParameters.pagination().getSize(), queryParameters.filter().getQueryString(), databaseFactory.getConnection(queryParameters.databaseScope()), credential);

    }


    public JsonDocument findInstanceByIdentifier(NexusSchemaReference schema, String identifier, DatabaseScope databaseScope, Credential credential) {
        NexusInstanceReference reference = arangoRepository.findBySchemaOrgIdentifier(ArangoCollectionReference.fromNexusSchemaReference(schema), identifier, databaseScope, credential);
        if (reference != null) {
            NexusInstanceReference originalId = arangoRepository.findOriginalId(reference, credential);
            if (originalId != null) {
                return getInstance(originalId.toSubSpace(SubSpace.MAIN), databaseFactory.getInferredDB(), credential).removeAllInternalKeys();
            }
        }
        return null;
    }


    public JsonDocument getInstanceByClientExtension(NexusInstanceReference instanceReference, String clientExtension, Client client, Credential credential) {
        NexusSchemaReference schemaReference = instanceReference.getNexusSchema().toSubSpace(client != null && client.getSubSpace() != null ? client.getSubSpace() : SubSpace.MAIN);
        NexusInstanceReference originalId = arangoRepository.findOriginalId(instanceReference, credential);
        JsonDocument instance = getInstance(originalId, databaseFactory.getDefaultDB(), credential);
        if (instance != null) {
            String identifier = constructIdentifierWithClientIdExtension(instance.getPrimaryIdentifier(), clientExtension);
            NexusInstanceReference bySchemaOrgIdentifier = arangoRepository.findBySchemaOrgIdentifier(ArangoCollectionReference.fromNexusSchemaReference(schemaReference), identifier, DatabaseScope.NATIVE, credential);
            if (bySchemaOrgIdentifier != null) {
                return new JsonDocument(arangoRepository.getDocument(ArangoDocumentReference.fromNexusInstance(bySchemaOrgIdentifier), databaseFactory.getDefaultDB(), credential)).removeAllInternalKeys();
            }
            return null;
        }
        return null;
    }

    private JsonDocument getInstance(NexusInstanceReference instanceReference, ArangoConnection connection, Credential credential) {
        return arangoRepository.getInstance(ArangoDocumentReference.fromNexusInstance(instanceReference), connection, credential);
    }


    public NexusInstanceReference createNewInstance(NexusSchemaReference nexusSchemaReference, String payload, Client client, Credential credential) {
        SubSpace subSpace = client != null ? client.getSubSpace() : SubSpace.MAIN;
        nexusSchemaReference = nexusSchemaReference.toSubSpace(subSpace);
        NexusInstanceReference newInstance = instanceController.createNewInstance(nexusSchemaReference, jsonTransformer.parseToMap(payload), credential);
        return newInstance.toSubSpace(SubSpace.MAIN);
    }

    private String constructIdentifierWithClientIdExtension(String identifier, String clientIdExtension) {
        return clientIdExtension != null ? identifier + clientIdExtension : identifier;

    }

    public NexusInstanceReference updateInstance(NexusInstanceReference instanceReference, String payload, Client client, String clientIdExtension, Credential credential) {
        NexusInstanceReference originalId = arangoRepository.findOriginalId(instanceReference, credential);
        JsonDocument instance = getInstance(originalId, databaseFactory.getDefaultDB(), credential);
        if (instance == null) {
            return null;
        }
        NexusSchemaReference nexusSchema = instanceReference.getNexusSchema();
        SubSpace subSpace = client != null ? client.getSubSpace() : SubSpace.MAIN;
        nexusSchema = nexusSchema.toSubSpace(subSpace);
        JsonDocument document = new JsonDocument(jsonTransformer.parseToMap(payload));
        String primaryIdentifier = instance.getPrimaryIdentifier();
        if (primaryIdentifier == null) {
            if (clientIdExtension == null && subSpace == originalId.getSubspace()) {
                //It's a replacement of the original instance - we therefore should be able to insert the data even without identifier mapping
                logger.warn(String.format("Found instance without identifier: %s - since it was an update for the original resource, I can continue - but please check what is happening here!", instanceReference.getRelativeUrl().getUrl()));
                return instanceController.createInstanceByNexusId(nexusSchema, originalId.getId(), instanceReference.getRevision() != null ? instanceReference.getRevision() : 1, document, credential);
            } else {
                throw new RuntimeException(String.format("Found instance without identifier: %s", instanceReference.getRelativeUrl().getUrl()));
            }

        }
        if (clientIdExtension != null || (subSpace != originalId.getSubspace())) {
            document.addReference(HBPVocabulary.INFERENCE_EXTENDS, nexusConfiguration.getAbsoluteUrl(originalId));
        }
        primaryIdentifier = constructIdentifierWithClientIdExtension(primaryIdentifier, clientIdExtension);
        return instanceController.createInstanceByIdentifier(nexusSchema, primaryIdentifier, document, credential);
    }

    public boolean removeInstance(NexusInstanceReference nexusInstanceReference, Credential credential) {
        NexusInstanceReference originalId = arangoRepository.findOriginalId(nexusInstanceReference, credential);
        //We only deprecate the original id - this way, the reconciled instance should disappear.
        return instanceController.deprecateInstanceByNexusId(originalId, credential);
    }

    public void cloneInstancesFromSchema(NexusSchemaReference originalSchema, String newVersion, Credential credential) {
        List<NexusInstanceReference> allInstancesForSchema = instanceController.getAllInstancesForSchema(originalSchema, credential);
        for (NexusInstanceReference instanceReference : allInstancesForSchema) {
            JsonDocument fromNexusById = instanceController.getFromNexusById(instanceReference, credential);
            //Ensure the right type
            fromNexusById.addType(schemaController.getTargetClass(originalSchema));
            //Redirect links
            JsonDocument redirectedJson = instanceController.pointLinksToSchema(fromNexusById, newVersion);
            NexusSchemaReference schemaReference = new NexusSchemaReference(originalSchema.getOrganization(), originalSchema.getDomain(), originalSchema.getSchema(), newVersion);
            instanceController.createInstanceByIdentifier(schemaReference, fromNexusById.getPrimaryIdentifier(), redirectedJson, credential);
        }
    }

    public void reindexInstancesFromSchema(NexusSchemaReference schemaReference, Credential credential) {

        nexusClient.consumeInstances(schemaReference, credential, true, instanceReferences -> {
            for (NexusInstanceReference instanceReference : instanceReferences) {
                if(instanceReference!=null){
                    JsonDocument fromNexusById = instanceController.getFromNexusById(instanceReference, credential);
                    //TODO extract userId from credential
                    IndexingMessage indexingMessage = new IndexingMessage(fromNexusById.getReference(), jsonTransformer.getMapAsJson(fromNexusById), ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT), null);
                    graphIndexing.update(indexingMessage);
                }
            }
        });

    }

    public void translateNamespaces(NexusSchemaReference schema, String oldNamespace, String newNamespace, Credential credential) {
        List<NexusInstanceReference> allInstancesForSchema = instanceController.getAllInstancesForSchema(schema, credential);
        for (NexusInstanceReference instanceReference : allInstancesForSchema) {
            JsonDocument fromNexusById = instanceController.getFromNexusById(instanceReference, credential);
            fromNexusById.replaceNamespace(oldNamespace, newNamespace);
            instanceController.createInstanceByNexusId(instanceReference.getNexusSchema(), instanceReference.getId(), instanceReference.getRevision(), fromNexusById, credential);
        }
    }


}
