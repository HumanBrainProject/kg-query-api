package org.humanbrainproject.knowledgegraph.instances.control;

import com.github.jsonldjava.core.JsonLdConsts;
import org.humanbrainproject.knowledgegraph.annotations.ToBeTested;
import org.humanbrainproject.knowledgegraph.commons.authorization.control.AuthorizationContext;
import org.humanbrainproject.knowledgegraph.commons.jsonld.control.JsonTransformer;
import org.humanbrainproject.knowledgegraph.commons.nexus.control.NexusClient;
import org.humanbrainproject.knowledgegraph.commons.nexus.control.NexusConfiguration;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.ArangoNativeRepository;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoCollectionReference;
import org.humanbrainproject.knowledgegraph.indexing.boundary.GraphIndexing;
import org.humanbrainproject.knowledgegraph.indexing.entity.IndexingMessage;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusInstanceReference;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusSchemaReference;
import org.humanbrainproject.knowledgegraph.query.entity.JsonDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@ToBeTested(integrationTestRequired = true)
@Component
public class InstanceMaintenanceController {

    @Autowired
    AuthorizationContext authorizationContext;

    @Autowired
    NexusClient nexusClient;

    @Autowired
    GraphIndexing graphIndexing;

    @Autowired
    JsonTransformer jsonTransformer;

    @Autowired
    ArangoNativeRepository arangoNativeRepository;

    @Autowired
    NexusConfiguration nexusConfiguration;

    @Autowired
    InstanceLookupController lookupController;

    @Autowired
    InstanceManipulationController manipulationController;

    private Logger logger = LoggerFactory.getLogger(InstanceMaintenanceController.class);

    public void cloneInstancesFromSchema(NexusSchemaReference originalSchema, String newVersion) {
        List<NexusInstanceReference> allInstancesForSchema = lookupController.getAllInstancesForSchema(originalSchema);
        for (NexusInstanceReference instanceReference : allInstancesForSchema) {
            JsonDocument fromNexusById = lookupController.getFromNexusById(instanceReference);
            //Ensure the right type
            fromNexusById.addType(SchemaController.getTargetClass(originalSchema));
            //Redirect links
            JsonDocument redirectedJson = pointLinksToSchema(fromNexusById, newVersion);
            NexusSchemaReference schemaReference = new NexusSchemaReference(originalSchema.getOrganization(), originalSchema.getDomain(), originalSchema.getSchema(), newVersion);
            manipulationController.createInstanceByIdentifier(schemaReference, fromNexusById.getPrimaryIdentifier(), redirectedJson, null);
        }
    }


    public void reindexInstancesFromSchema(NexusSchemaReference schemaReference) {
        nexusClient.consumeInstances(schemaReference, authorizationContext.getCredential(), true, instanceReferences -> {
            for (NexusInstanceReference instanceReference : instanceReferences) {
                if (instanceReference != null) {
                    JsonDocument fromNexusById = lookupController.getFromNexusById(instanceReference);
                    //TODO extract userId from credential
                    IndexingMessage indexingMessage = new IndexingMessage(fromNexusById.getReference(), jsonTransformer.getMapAsJson(fromNexusById), ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT), null);
                    graphIndexing.update(indexingMessage);
                }
            }
        });
    }


    public void translateNamespaces(NexusSchemaReference schema, String oldNamespace, String newNamespace) {
        List<NexusInstanceReference> allInstancesForSchema = lookupController.getAllInstancesForSchema(schema);
        for (NexusInstanceReference instanceReference : allInstancesForSchema) {
            JsonDocument fromNexusById = lookupController.getFromNexusById(instanceReference);
            fromNexusById.replaceNamespace(oldNamespace, newNamespace);
            manipulationController.createInstanceByNexusId(instanceReference.getNexusSchema(), instanceReference.getId(), instanceReference.getRevision(), fromNexusById, null);
        }
    }


    private JsonDocument pointLinksToSchema(JsonDocument jsonDocument, String newVersion) {
        JsonDocument newDocument = new JsonDocument(jsonDocument);
        newDocument.processLinks(referenceMap -> {
            NexusInstanceReference related = NexusInstanceReference.createFromUrl((String) referenceMap.get(JsonLdConsts.ID));
            if (related != null) {
                NexusSchemaReference schema = related.getNexusSchema();
                NexusSchemaReference newSchemaReference = new NexusSchemaReference(schema.getOrganization(), schema.getDomain(), schema.getSchema(), newVersion);
                JsonDocument relatedDocument = nexusClient.get(related.getRelativeUrl(), authorizationContext.getCredential());
                if (relatedDocument != null) {
                    String primaryIdentifier = relatedDocument.getPrimaryIdentifier();
                    NexusInstanceReference inNewSchema = arangoNativeRepository.findBySchemaOrgIdentifier(ArangoCollectionReference.fromNexusSchemaReference(newSchemaReference), primaryIdentifier);
                    if (inNewSchema != null) {
                        referenceMap.put(JsonLdConsts.ID, nexusConfiguration.getAbsoluteUrl(inNewSchema));
                    }
                }
            }
        });
        return newDocument;
    }
}
