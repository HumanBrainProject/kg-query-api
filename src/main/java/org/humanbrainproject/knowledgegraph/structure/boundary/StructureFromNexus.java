package org.humanbrainproject.knowledgegraph.structure.boundary;

import org.humanbrainproject.knowledgegraph.commons.authorization.control.AuthorizationController;
import org.humanbrainproject.knowledgegraph.commons.authorization.entity.InternalMasterKey;
import org.humanbrainproject.knowledgegraph.commons.nexus.control.NexusClient;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.entity.SubSpace;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusSchemaReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

@Component
public class StructureFromNexus {

    @Autowired
    NexusClient nexusClient;

    @Autowired
    AuthorizationController authorizationController;

    public Set<NexusSchemaReference> getAllSchemasInMainSpace() {
        Set<NexusSchemaReference> allSchemas = nexusClient.getAllSchemas(null, null, authorizationController.getInterceptor(new InternalMasterKey()));
        return allSchemas.stream().map(s -> s.toSubSpace(SubSpace.MAIN)).collect(Collectors.toSet());
    }
}
