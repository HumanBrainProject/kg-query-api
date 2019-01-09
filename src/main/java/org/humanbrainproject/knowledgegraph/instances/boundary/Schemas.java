package org.humanbrainproject.knowledgegraph.instances.boundary;

import org.humanbrainproject.knowledgegraph.annotations.ToBeTested;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.entity.SubSpace;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusSchemaReference;
import org.humanbrainproject.knowledgegraph.instances.control.SchemaController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@ToBeTested(integrationTestRequired = true, systemTestRequired = true)
@Component
public class Schemas {

    @Autowired
    SchemaController schemaController;

    public void createSimpleSchema(NexusSchemaReference nexusSchemaReference) {
        schemaController.createSchema(nexusSchemaReference);
    }


    public void clearAllInstancesOfSchema(NexusSchemaReference nexusSchemaReference){
        for (SubSpace subSpace : SubSpace.values()) {
            NexusSchemaReference subSpaceSchema = nexusSchemaReference.toSubSpace(subSpace);
            schemaController.clearAllInstancesFromSchema(subSpaceSchema);
        }
    }


    public void createSchemasInNewVersion(String org, String newVersion){
        List<NexusSchemaReference> allSchemas = schemaController.getAllSchemas(org);
        for (NexusSchemaReference schema : allSchemas) {
            NexusSchemaReference newReference = new NexusSchemaReference(schema.getOrganization(), schema.getDomain(), schema.getSchema(), newVersion);
            createSimpleSchema(newReference);
        }
    }


}
