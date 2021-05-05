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

package org.humanbrainproject.knowledgegraph.instances.boundary;

import org.apache.solr.client.solrj.SolrServerException;
import org.humanbrainproject.knowledgegraph.annotations.ToBeTested;
import org.humanbrainproject.knowledgegraph.commons.jsonld.control.JsonTransformer;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.entity.SubSpace;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusInstanceReference;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusSchemaReference;
import org.humanbrainproject.knowledgegraph.instances.control.InstanceLookupController;
import org.humanbrainproject.knowledgegraph.instances.control.InstanceManipulationController;
import org.humanbrainproject.knowledgegraph.instances.control.InstanceMaintenanceController;
import org.humanbrainproject.knowledgegraph.query.entity.JsonDocument;
import org.humanbrainproject.knowledgegraph.query.entity.Pagination;
import org.humanbrainproject.knowledgegraph.query.entity.QueryResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.configurationprocessor.json.JSONException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

@ToBeTested(integrationTestRequired = true, systemTestRequired = true)
@Component
public class Instances {

    @Autowired
    InstanceLookupController lookupController;

    @Autowired
    InstanceManipulationController manipulationController;

    @Autowired
    InstanceMaintenanceController instanceMaintenanceController;

    @Autowired
    JsonTransformer jsonTransformer;

    private Logger logger = LoggerFactory.getLogger(Instances.class);

    public List<Map> getLinkingInstances(NexusInstanceReference fromInstance, NexusInstanceReference toInstance, NexusSchemaReference relationType){
        return lookupController.getLinkingInstances(fromInstance, toInstance, relationType);
    }

    public JsonDocument getInstance(NexusInstanceReference instanceReference) {
       return lookupController.getInstance(instanceReference);
    }

    public QueryResult<List<Map>> getInstances(NexusSchemaReference schemaReference, String searchTerm, Pagination pagination) {
        return lookupController.getInstances(schemaReference, searchTerm, pagination);
    }

    public JsonDocument findInstanceByIdentifier(NexusSchemaReference schema, String identifier) {
        return lookupController.findInstanceByIdentifier(schema, identifier);
    }

    public JsonDocument getInstanceByClientExtension(NexusInstanceReference instanceReference, String clientExtension) {
        return lookupController.getInstanceByClientExtension(instanceReference, clientExtension);
    }

    public NexusInstanceReference createNewInstance(NexusSchemaReference nexusSchemaReference, String payload, String clienIdExtension) {
        return manipulationController.createNewInstance(nexusSchemaReference, jsonTransformer.parseToMap(payload), clienIdExtension).toSubSpace(SubSpace.MAIN);
    }

    public NexusInstanceReference updateInstance(NexusInstanceReference instanceReference, String payload, String clientIdExtension) {
       return manipulationController.updateInstance(instanceReference, jsonTransformer.parseToMap(payload), clientIdExtension);
    }

    public boolean removeInstance(NexusInstanceReference nexusInstanceReference) {
        return manipulationController.removeInstance(nexusInstanceReference);
    }

    public void cloneInstancesFromSchema(NexusSchemaReference originalSchema, String newVersion) {
        instanceMaintenanceController.cloneInstancesFromSchema(originalSchema, newVersion);
    }

    public void reindexInstancesFromSchema(NexusSchemaReference schemaReference) {
        instanceMaintenanceController.reindexInstancesFromSchema(schemaReference);
    }

    public void translateNamespaces(NexusSchemaReference schema, String oldNamespace, String newNamespace) {
        instanceMaintenanceController.translateNamespaces(schema, oldNamespace, newNamespace);
    }

    public List<Map> getInstancesByReferences(Set<NexusInstanceReference> references, String queryId, String vocab, Map<String, String> queryParams) throws SolrServerException, JSONException, IOException {
        return lookupController.getInstancesByReferences(references, queryId, vocab, queryParams);
    }

}
