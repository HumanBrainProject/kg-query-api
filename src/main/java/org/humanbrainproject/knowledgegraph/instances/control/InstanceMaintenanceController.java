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
    SchemaController schemaController;

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
            fromNexusById.addType(schemaController.getTargetClass(originalSchema));
            //Redirect links
            JsonDocument redirectedJson = pointLinksToSchema(fromNexusById, newVersion);
            NexusSchemaReference schemaReference = new NexusSchemaReference(originalSchema.getOrganization(), originalSchema.getDomain(), originalSchema.getSchema(), newVersion);
            manipulationController.createInstanceByIdentifier(schemaReference, fromNexusById.getPrimaryIdentifier(), redirectedJson, authorizationContext.getUserId());
        }
    }


    public void reindexInstancesFromSchema(NexusSchemaReference schemaReference) {
        nexusClient.consumeInstances(schemaReference, authorizationContext.getCredential(), true, instanceReferences -> {
            for (NexusInstanceReference instanceReference : instanceReferences) {
                if (instanceReference != null) {
                    JsonDocument fromNexusById = lookupController.getFromNexusById(instanceReference);
                    IndexingMessage indexingMessage = new IndexingMessage(fromNexusById.getReference(), jsonTransformer.getMapAsJson(fromNexusById), ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT), authorizationContext.getUserId());
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
            manipulationController.createInstanceByNexusId(instanceReference.getNexusSchema(), instanceReference.getId(), instanceReference.getRevision(), fromNexusById, authorizationContext.getUserId());
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
