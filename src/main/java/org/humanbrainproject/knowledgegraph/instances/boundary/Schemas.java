package org.humanbrainproject.knowledgegraph.instances.boundary;

import org.humanbrainproject.knowledgegraph.commons.authorization.entity.OidcAccessToken;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.entity.SubSpace;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusSchemaReference;
import org.humanbrainproject.knowledgegraph.instances.control.SchemaController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class Schemas {

    @Autowired
    SchemaController schemaController;


    public void createSimpleSchema(NexusSchemaReference nexusSchemaReference) {
        schemaController.createSchema(nexusSchemaReference);
    }


    public void clearAllInstancesOfSchema(NexusSchemaReference nexusSchemaReference, OidcAccessToken oidcAccessToken){
        for (SubSpace subSpace : SubSpace.values()) {
            NexusSchemaReference subSpaceSchema = nexusSchemaReference.toSubSpace(subSpace);
            schemaController.clearAllInstancesFromSchema(subSpaceSchema, oidcAccessToken);
        }
    }


    public void createSchemasInNewVersion(String org, String newVersion, OidcAccessToken oidcAccessToken){
        List<NexusSchemaReference> allSchemas = schemaController.getAllSchemas(org, oidcAccessToken);
        for (NexusSchemaReference schema : allSchemas) {
            NexusSchemaReference newReference = new NexusSchemaReference(schema.getOrganization(), schema.getDomain(), schema.getSchema(), newVersion);
            createSimpleSchema(newReference);
        }
    }


}
