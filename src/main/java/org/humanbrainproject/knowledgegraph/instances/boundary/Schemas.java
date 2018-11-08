package org.humanbrainproject.knowledgegraph.instances.boundary;

import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusSchemaReference;
import org.humanbrainproject.knowledgegraph.instances.control.SchemaController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class Schemas {

    @Autowired
    SchemaController schemaController;


    public void createSimpleSchema(NexusSchemaReference nexusSchemaReference) {
        schemaController.createSchema(nexusSchemaReference);
    }


}
