package org.humanbrainproject.knowledgegraph.indexing.control.inference;

import com.github.jsonldjava.core.JsonLdConsts;
import org.humanbrainproject.knowledgegraph.commons.nexus.control.NexusConfiguration;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.ArangoDatabaseFactory;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.ArangoRepository;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.entity.*;
import org.humanbrainproject.knowledgegraph.indexing.control.IndexingProvider;
import org.humanbrainproject.knowledgegraph.indexing.control.MessageProcessor;
import org.humanbrainproject.knowledgegraph.indexing.entity.InstanceReference;
import org.humanbrainproject.knowledgegraph.indexing.entity.QualifiedIndexingMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class Reconciliation implements InferenceStrategy, InitializingBean {


    public final static String ORIGINAL_PARENT_PROPERTY = "http://hbp.eu/reconciled#original_parent";

    @Autowired
    InferenceController controller;

    @Autowired
    ArangoDatabaseFactory databaseController;

    @Autowired
    ArangoRepository repository;

    @Autowired
    NexusConfiguration nexusConfiguration;

    @Autowired
    MessageProcessor graphSpecificationController;

    @Autowired
    IndexingProvider indexingProvider;

    protected Logger logger = LoggerFactory.getLogger(Reconciliation.class);


    @Override
    public void afterPropertiesSet() {
        controller.addInferenceStrategy(this);
    }

    @Override
    public void infer(QualifiedIndexingMessage message, Set<MainVertex> documents) {
        //We collect all instances from the default space
        InstanceReference originalId = indexingProvider.findOriginalId(message.getOriginalMessage().getInstanceReference());
        Set<? extends InstanceReference> relativeInstances = indexingProvider.findInstancesWithLinkTo(ORIGINAL_PARENT_PROPERTY, originalId, ReferenceType.INTERNAL);
        if (!relativeInstances.isEmpty()) {
            MainVertex originalStructure = indexingProvider.getVertexStructureById(originalId);
            if (originalStructure != null) {
                Set<MainVertex> relativeInstanceStructures = relativeInstances != null ? relativeInstances.stream().map(relativeInstance -> {
                    return indexingProvider.getVertexStructureById(relativeInstance);

                }).collect(Collectors.toSet()) : Collections.emptySet();
                //Now we apply the inference logic
                MainVertex newVertex = mergeInstances(originalStructure, new ArrayList<>(relativeInstanceStructures));
                //And we add the inferred instance to the collection of documents
                documents.add(newVertex);
            }
        }
    }

    private MainVertex mergeInstances(MainVertex original, List<MainVertex> additionalInstances) {
        MainVertex newVertex = new MainVertex(original.getInstanceReference());
        Property<List<String>> types = Property.createProperty(JsonLdConsts.TYPE, new ArrayList<String>());
        types.getValue().addAll(original.getTypes());
        types.getValue().add(InferenceController.INFERRED_TYPE);
        newVertex.getProperties().add(types);
        mergeVertex(newVertex, original, additionalInstances, newVertex);
        for (MainVertex additionalInstance : additionalInstances) {
            mergeVertex(newVertex, additionalInstance, additionalInstances.stream().filter(i -> i != additionalInstance).collect(Collectors.toList()), newVertex);
        }
        return newVertex;
    }

    private void mergeVertex(Vertex newVertex, Vertex vertex, List<? extends Vertex> additionalInstances, MainVertex mainVertex) {
        //Add all properties of the original instance
        for (Property property : vertex.getProperties()) {
            //It is important, that we only take care of properties that not have been addressed before.
            if (newVertex.getPropertyByName(property.getName()) == null) {
                mergeProperty(newVertex, property, additionalInstances.stream().map(i -> i.getPropertyByName(property.getName())).collect(Collectors.toList()));
            }
        }
        int orderNumber = 0;
        for (Edge edge : vertex.getEdges()) {
            if (newVertex.getEdgeByName(edge.getTypeName()) == null) {
                //merge edge
                if (edge instanceof EmbeddedEdge) {
                    EmbeddedEdge newEmbeddedEdge = new EmbeddedEdge(edge.getTypeName(), newVertex, orderNumber++, mainVertex);
                    EmbeddedVertex embeddedVertex = new EmbeddedVertex(newEmbeddedEdge);
                    List<Vertex> alternativeEmbeddedInstances = additionalInstances.stream().map(i -> i.getEdgeByName(edge.getTypeName())).filter(e -> e instanceof EmbeddedEdge).map(e -> ((EmbeddedEdge) e).getToVertex()).collect(Collectors.toList());
                    mergeVertex(embeddedVertex, ((EmbeddedEdge) edge).getToVertex(), alternativeEmbeddedInstances, mainVertex);
                } else {
                    mergeReference(newVertex, edge, additionalInstances.stream().map(i -> i.getEdgeByName(edge.getTypeName())).filter(Objects::nonNull).collect(Collectors.toList()));
                }
            }
        }
    }

    private void mergeReference(Vertex newVertex, Edge edge, List<? extends Edge> additionalEdges) {
        if (additionalEdges.isEmpty()) {
            newVertex.getEdges().add(edge);
        }
    }


    private void mergeProperty(Vertex newVertex, Property property, List<? extends Property> additionalProperties) {
        if (additionalProperties.isEmpty()) {
            newVertex.getProperties().add(property);
        }
    }


}
