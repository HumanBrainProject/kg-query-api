package org.humanbrainproject.knowledgegraph.instances.boundary;

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
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

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

    public NexusInstanceReference createNewInstance(NexusSchemaReference nexusSchemaReference, String payload) {
        return manipulationController.createNewInstance(nexusSchemaReference, jsonTransformer.parseToMap(payload)).toSubSpace(SubSpace.MAIN);
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

}
