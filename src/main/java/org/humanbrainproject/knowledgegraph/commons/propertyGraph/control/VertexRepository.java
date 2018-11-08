package org.humanbrainproject.knowledgegraph.commons.propertyGraph.control;

import org.humanbrainproject.knowledgegraph.commons.nexus.control.NexusConfiguration;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.entity.Vertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;


public abstract class VertexRepository<Connection extends DatabaseConnection<?>, InternalDocumentReference> {

    public static final String UNRESOLVED_LINKS = "https://schema.hbp.eu/propertygraph/unresolved";

    @Autowired
    NexusConfiguration nexusConfiguration;

    protected Logger logger = LoggerFactory.getLogger(VertexRepository.class);

    public abstract Vertex getVertexStructureById(InternalDocumentReference internalDocumentReference, Connection connection);

    public abstract void clearDatabase(Connection connection);


}
