package org.humanbrainproject.knowledgegraph.instances.boundary;

import org.humanbrainproject.knowledgegraph.commons.authorization.entity.OidcAccessToken;
import org.humanbrainproject.knowledgegraph.commons.jsonld.control.JsonLdStandardization;
import org.humanbrainproject.knowledgegraph.commons.jsonld.control.JsonTransformer;
import org.humanbrainproject.knowledgegraph.commons.nexus.control.NexusConfiguration;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.ArangoConnection;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.ArangoDatabaseFactory;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.ArangoRepository;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoDocumentReference;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.entity.SubSpace;
import org.humanbrainproject.knowledgegraph.commons.vocabulary.HBPVocabulary;
import org.humanbrainproject.knowledgegraph.indexing.control.MessageProcessor;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusInstanceReference;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusSchemaReference;
import org.humanbrainproject.knowledgegraph.instances.control.InstanceController;
import org.humanbrainproject.knowledgegraph.instances.entity.Client;
import org.humanbrainproject.knowledgegraph.query.entity.JsonDocument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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


    public JsonDocument getInstance(NexusInstanceReference instanceReference) {
        NexusInstanceReference originalId = arangoRepository.findOriginalId(instanceReference);
        return getInstance(originalId, databaseFactory.getInferredDB());
    }


    private JsonDocument getInstance(NexusInstanceReference instanceReference, ArangoConnection connection) {
        return arangoRepository.getInstance(ArangoDocumentReference.fromNexusInstance(instanceReference), connection);
    }


    public NexusInstanceReference createNewInstance(NexusSchemaReference nexusSchemaReference, OidcAccessToken oidcAccessToken) {
        return instanceController.createNewEmptyInstance(nexusSchemaReference, oidcAccessToken);
    }

    public NexusInstanceReference updateInstance(NexusInstanceReference instanceReference, String payload, Client client, String clientIdExtension, OidcAccessToken oidcAccessToken) {
        NexusInstanceReference originalId = arangoRepository.findOriginalId(instanceReference);
        JsonDocument instance = getInstance(originalId, databaseFactory.getDefaultDB());
        if (instance == null) {
            return null;
        }
        NexusSchemaReference nexusSchema = instanceReference.getNexusSchema();
        SubSpace subSpace = client != null ? client.getSubSpace() : SubSpace.MAIN;
        JsonDocument document = new JsonDocument(jsonTransformer.parseToMap(payload));
        String primaryIdentifier = instance.getPrimaryIdentifier();
        if (primaryIdentifier == null) {
            throw new RuntimeException(String.format("Found instance without identifier: %s", instanceReference.getRelativeUrl()));
        }
        if (clientIdExtension != null || (subSpace != originalId.getSubspace())) {
            document.addReference(HBPVocabulary.INFERENCE_EXTENDS, nexusConfiguration.getAbsoluteUrl(originalId));
        }
        if (clientIdExtension != null) {
            primaryIdentifier += clientIdExtension;
        }
        return instanceController.createInstanceByIdentifier(nexusSchema, primaryIdentifier, document, oidcAccessToken);
    }

    public boolean removeInstance(NexusInstanceReference nexusInstanceReference, OidcAccessToken oidcAccessToken) {
        NexusInstanceReference originalId = arangoRepository.findOriginalId(nexusInstanceReference);
        //We only deprecate the original id - this way, the reconciled instance should disappear.
        return instanceController.deprecateInstanceByNexusId(originalId, oidcAccessToken);
    }


}
