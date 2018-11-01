package org.humanbrainproject.knowledgegraph.nexusExt.boundary;

import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusSchemaReference;
import org.humanbrainproject.knowledgegraph.nexusExt.control.SchemaController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class NexusExtension {

    @Autowired
    SchemaController schemaController;


    public void createSimpleSchema(NexusSchemaReference nexusSchemaReference) throws IOException {
        schemaController.createSchema(nexusSchemaReference);
    }


}
