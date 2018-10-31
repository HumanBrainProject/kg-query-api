package org.humanbrainproject.knowledgegraph.indexing.control.inference;

import com.github.jsonldjava.core.JsonLdConsts;
import deprecated.exceptions.InferenceException;
import org.humanbrainproject.knowledgegraph.commons.nexus.control.NexusConfiguration;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.ArangoDatabaseFactory;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.ArangoRepository;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.entity.*;
import org.humanbrainproject.knowledgegraph.indexing.control.IndexingProvider;
import org.humanbrainproject.knowledgegraph.indexing.control.MessageProcessor;
import org.humanbrainproject.knowledgegraph.indexing.entity.QualifiedIndexingMessage;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusInstanceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class Reconciliation implements InferenceStrategy, InitializingBean {

    private final static List<String> NAME_BLACKLIST_FOR_MERGE = Arrays.asList(JsonLdConsts.ID, InferenceController.ORIGINAL_PARENT_PROPERTY);

    @Autowired
    InferenceController controller;

    @Autowired
    ArangoDatabaseFactory databaseController;

    @Autowired
    ArangoRepository repository;

    @Autowired
    NexusConfiguration nexusConfiguration;

    @Autowired
    MessageProcessor messageProcessor;

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
        NexusInstanceReference originalId = message.getOriginalId();
        boolean isOriginal = originalId.equals(message.getOriginalMessage().getInstanceReference());

        Set<NexusInstanceReference> relativeInstances = indexingProvider.findInstancesWithLinkTo(InferenceController.ORIGINAL_PARENT_PROPERTY, originalId, ReferenceType.INTERNAL);
        Set<NexusInstanceReference> inferredInstances = indexingProvider.findInstancesWithLinkTo(InferenceController.INFERENCE_OF_PROPERTY, originalId, ReferenceType.INTERNAL);
        if (!isOriginal || (relativeInstances!=null && !relativeInstances.isEmpty())) {
            MainVertex originalStructure = indexingProvider.getVertexStructureById(originalId);
            if (originalStructure != null) {
                List<MainVertex> relativeStructures = relativeInstances.stream().filter(r -> r.equals(message.getOriginalMessage().getInstanceReference())).map(relativeInstance -> indexingProvider.getVertexStructureById(relativeInstance)).collect(Collectors.toList());
                if(!isOriginal) {
                    //Add the new message as part of the relative structure
                    relativeStructures.add(messageProcessor.createVertexStructure(message).getMainVertex());
                }
                //Now we apply the inference logic
                MainVertex newVertex = mergeInstances(originalStructure, new ArrayList<>(relativeStructures), inferredInstances);
                //And we add the inferred instance to the collection of documents
                documents.add(newVertex);
            }
        }
    }





    private MainVertex mergeInstances(MainVertex original, List<MainVertex> additionalInstances, Set<NexusInstanceReference> inferredInstances) {
        MainVertex newVertex;
        if(inferredInstances!=null && !inferredInstances.isEmpty()){
            if(inferredInstances.size()==1){
                newVertex = new MainVertex(inferredInstances.iterator().next());
            }
            else{
                throw new InferenceException(String.format("Multiple inferred entities for the original entity %s", original.getInstanceReference().getFullId(true)));
            }
        }
        else{
            //There is no inferred instance yet - so we create a new one.
            newVertex = new MainVertex(new NexusInstanceReference(original.getInstanceReference().getNexusSchema(), null).toSubSpace(SubSpace.INFERRED));
        }
        newVertex.getEdges().add(new InternalEdge(InferenceController.INFERENCE_OF_PROPERTY, newVertex, original.getInstanceReference(), 0, newVertex));
        //newVertex.getProperties().add(Property.createReference(INFERENCE_OF_PROPERTY, nexusConfiguration.getAbsoluteUrl(original.getInstanceReference())));

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
        if (additionalEdges.isEmpty() && !NAME_BLACKLIST_FOR_MERGE.contains(edge.getName())) {
            newVertex.getEdges().add(edge);
        }
    }


    private void mergeProperty(Vertex newVertex, Property property, List<? extends Property> additionalProperties) {
        if (additionalProperties.isEmpty() && !NAME_BLACKLIST_FOR_MERGE.contains(property.getName())) {
            newVertex.getProperties().add(property);
        }
    }


}
