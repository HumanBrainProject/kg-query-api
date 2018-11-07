package org.humanbrainproject.knowledgegraph.instances.boundary;

import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.ArangoDatabaseFactory;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.ArangoRepository;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoDocumentReference;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusInstanceReference;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusSchemaReference;
import org.humanbrainproject.knowledgegraph.instances.control.SchemaController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class Instances {

    @Autowired
    SchemaController schemaController;

    @Autowired
    ArangoRepository arangoRepository;

    @Autowired
    ArangoDatabaseFactory databaseFactory;


    public void createSimpleSchema(NexusSchemaReference nexusSchemaReference) {
        schemaController.createSchema(nexusSchemaReference);
    }

    public Map getInstance(NexusInstanceReference instanceReference){
        return arangoRepository.getInstance(ArangoDocumentReference.fromNexusInstance(instanceReference), databaseFactory.getInferredDB());
    }



}
