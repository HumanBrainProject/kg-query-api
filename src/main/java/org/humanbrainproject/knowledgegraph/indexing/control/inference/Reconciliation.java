package org.humanbrainproject.knowledgegraph.indexing.control.inference;

import com.github.jsonldjava.core.JsonLdConsts;
import deprecated.exceptions.InferenceException;
import org.humanbrainproject.knowledgegraph.commons.nexus.control.NexusConfiguration;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.ArangoDatabaseFactory;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.ArangoRepository;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.entity.*;
import org.humanbrainproject.knowledgegraph.commons.vocabulary.HBPVocabulary;
import org.humanbrainproject.knowledgegraph.indexing.control.MessageProcessor;
import org.humanbrainproject.knowledgegraph.indexing.control.nexusToArango.NexusToArangoIndexingProvider;
import org.humanbrainproject.knowledgegraph.indexing.entity.QualifiedIndexingMessage;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusInstanceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class Reconciliation implements InferenceStrategy, InitializingBean {

    private final static List<String> NAME_BLACKLIST_FOR_MERGE = Arrays.asList(JsonLdConsts.ID, HBPVocabulary.INFERENCE_EXTENDS);

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
    NexusToArangoIndexingProvider indexingProvider;

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

        Set<NexusInstanceReference> relativeInstances = indexingProvider.findInstancesWithLinkTo(HBPVocabulary.INFERENCE_EXTENDS, originalId, ReferenceType.INTERNAL);
        Set<NexusInstanceReference> inferredInstances = indexingProvider.findInstancesWithLinkTo(HBPVocabulary.INFERENCE_OF, originalId, ReferenceType.INTERNAL);
        if (!isOriginal || (relativeInstances != null && !relativeInstances.isEmpty())) {
            MainVertex originalStructure;
            if (isOriginal) {
                originalStructure = messageProcessor.createVertexStructure(message).getMainVertex();
            } else {
                originalStructure = indexingProvider.getVertexStructureById(originalId);
            }
            if (originalStructure != null) {
                List<MainVertex> relativeStructures = relativeInstances.stream().filter(r -> r.equals(message.getOriginalMessage().getInstanceReference())).map(relativeInstance -> indexingProvider.getVertexStructureById(relativeInstance)).collect(Collectors.toList());
                if (!isOriginal) {
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
        if (inferredInstances != null && !inferredInstances.isEmpty()) {
            if (inferredInstances.size() == 1) {
                newVertex = new MainVertex(inferredInstances.iterator().next());
            } else {
                throw new InferenceException(String.format("Multiple inferred entities for the original entity %s", original.getInstanceReference().getFullId(true)));
            }
        } else {
            //There is no inferred instance yet - so we create a new one.
            newVertex = new MainVertex(new NexusInstanceReference(original.getInstanceReference().getNexusSchema(), null).toSubSpace(SubSpace.INFERRED));
        }
        newVertex.getEdges().add(new InternalEdge(HBPVocabulary.INFERENCE_OF, newVertex, original.getInstanceReference(), 0, newVertex));
        //newVertex.getProperties().add(Property.createReference(INFERENCE_OF, nexusConfiguration.getAbsoluteUrl(original.getInstanceReference())));

        List<String> types = new ArrayList<>(original.getTypes());
        types.add(HBPVocabulary.INFERENCE_TYPE);
        newVertex.getProperties().add(Property.createProperty(JsonLdConsts.TYPE, types));
        //Set<MainVertex> allVertices = new HashSet<>();
        //allVertices.add(original);
        //allVertices.addAll(additionalInstances);
        //mergeVertex(newVertex, allVertices);
        mergeVertex(newVertex, original, additionalInstances, newVertex);
        for (MainVertex additionalInstance : additionalInstances) {
            mergeVertex(newVertex, additionalInstance, additionalInstances.stream().filter(i -> i != additionalInstance).collect(Collectors.toList()), newVertex);
        }
        return newVertex;
    }


    private void mergeVertex(Vertex newVertex, Set<? extends Vertex> vertices) {
        Set<String> handledProperties = new HashSet<>();
        for (Vertex vertex : vertices) {
            for (Property property : vertex.getProperties()) {
                if (!handledProperties.contains(property.getName())) {
                    newVertex.getProperties().add(mergeProperty(property.getName(), vertices));
                    handledProperties.add(property.getName());
                }
            }
        }
    }




    private int compareVertexPower(Vertex newVertex, Vertex oldVertex) {
        SubSpace subspaceNew = newVertex == null ? null : newVertex.getMainVertex().getInstanceReference().getSubspace();
        SubSpace subspaceOld = oldVertex == null ? null : oldVertex.getMainVertex().getInstanceReference().getSubspace();
        if (subspaceNew == subspaceOld) {
            return 0;
        } else if (SubSpace.EDITOR == subspaceNew) {
            return 1;
        } else {
            return -1;
        }
    }

    private LocalDateTime getIndexedAt(MainVertex vertex){
        Object indexedAt = vertex.getPropertyByName(HBPVocabulary.PROVENANCE_INDEXED_IN_ARANGO_AT).getValue();
        if(indexedAt!=null){
            return LocalDateTime.from(DateTimeFormatter.ISO_INSTANT.parse(indexedAt.toString()));
        }
        return null;
    }


    private boolean overrides(Vertex potentialOverride, Vertex currentVertex, Object currentValue, String propertyName, Map<Object, Integer> valueCount){
        int i = compareVertexPower(potentialOverride, currentVertex);
        Property currentProperty = potentialOverride.getPropertyByName(propertyName);
        Integer count = valueCount.get(currentProperty.getValue());
        if (count == null) {
            count = 0;
        }
        count += 1;
        valueCount.put(currentProperty.getValue(), count);
        boolean overrides = currentValue == null;
        if (i > 0) {
            overrides = true;
        } else if (i == 0 && !overrides) {
            Integer counts = valueCount.get(currentProperty.getValue());
            Integer countsOfCurrentResult = valueCount.get(currentValue);
            if (counts > countsOfCurrentResult) {
                overrides = true;
            } else if (counts.intValue() == countsOfCurrentResult.intValue()) {
                LocalDateTime indexedAt = getIndexedAt(potentialOverride.getMainVertex());
                if(currentVertex!=null){
                    if(indexedAt != null){
                        LocalDateTime originIndexedAt = getIndexedAt(currentVertex.getMainVertex());
                        if(originIndexedAt==null || indexedAt.isAfter(originIndexedAt)){
                            overrides = true;
                        }
                    }
                }
                else{
                    overrides = true;
                }
            }
        }
        return overrides;
    }


    private Property mergeProperty(String propertyName, Set<? extends Vertex> vertices) {
        List<Vertex> verticesWithProperty = vertices.stream().filter(v -> v.getPropertyByName(propertyName) != null).collect(Collectors.toList());
        Property result = null;
        Vertex originOfResult = null;
        Set<Property<?>> alternatives = new LinkedHashSet<>();
        Map<Object, Integer> valueCount = new HashMap<>();
        for (Vertex vertex : verticesWithProperty) {
            if (overrides(vertex, originOfResult, result, propertyName, valueCount)) {
                if (result != null) {
                    alternatives.add(result);
                }
                result = vertex.getPropertyByName(propertyName);
                originOfResult = vertex;
            }
        }
        Property<?> property = Property.createProperty(propertyName, result.getValue());
        property.setAlternatives(alternatives);
        return property;
    }


    private void mergeVertex(Vertex newVertex, Vertex vertex, List<? extends Vertex> additionalInstances, MainVertex mainVertex) {
        //Add all properties of the original instance
        for (Property property : vertex.getProperties()) {
            //It is important, that we only take care of properties that not have been addressed before.
            if (newVertex.getPropertyByName(property.getName()) == null) {
                mergeProperty(newVertex, property, additionalInstances.stream().map(i -> i.getPropertyByName(property.getName())).filter(i -> i != null && !i.equals(property.getValue())).collect(Collectors.toSet()));
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


    private void mergeProperty(Vertex newVertex, Property property, Set<? extends Property> additionalProperties) {
        if (!NAME_BLACKLIST_FOR_MERGE.contains(property.getName())) {
            newVertex.getProperties().add(property);
            property.setAlternatives(additionalProperties);
        }
    }


}
