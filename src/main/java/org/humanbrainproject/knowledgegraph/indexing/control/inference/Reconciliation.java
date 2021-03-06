/*
 * Copyright 2018 - 2021 Swiss Federal Institute of Technology Lausanne (EPFL)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * This open source software code was developed in part or in whole in the
 * Human Brain Project, funded from the European Union's Horizon 2020
 * Framework Programme for Research and Innovation under
 * Specific Grant Agreements No. 720270, No. 785907, and No. 945539
 * (Human Brain Project SGA1, SGA2 and SGA3).
 *
 */

package org.humanbrainproject.knowledgegraph.indexing.control.inference;

import com.github.jsonldjava.core.JsonLdConsts;
import org.humanbrainproject.knowledgegraph.annotations.ToBeTested;
import org.humanbrainproject.knowledgegraph.commons.jsonld.control.JsonTransformer;
import org.humanbrainproject.knowledgegraph.commons.nexus.control.NexusConfiguration;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.ArangoNativeRepository;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.ArangoRepository;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.entity.Property;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.entity.SubSpace;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.entity.Vertex;
import org.humanbrainproject.knowledgegraph.commons.vocabulary.HBPVocabulary;
import org.humanbrainproject.knowledgegraph.commons.vocabulary.SchemaOrgVocabulary;
import org.humanbrainproject.knowledgegraph.indexing.control.MessageProcessor;
import org.humanbrainproject.knowledgegraph.indexing.control.nexusToArango.NexusToArangoIndexingProvider;
import org.humanbrainproject.knowledgegraph.indexing.entity.Alternative;
import org.humanbrainproject.knowledgegraph.indexing.entity.IndexingMessage;
import org.humanbrainproject.knowledgegraph.indexing.entity.QualifiedIndexingMessage;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusInstanceReference;
import org.humanbrainproject.knowledgegraph.indexing.exception.InferenceException;
import org.humanbrainproject.knowledgegraph.query.entity.JsonDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * The reconciliation is the first inference logic - instances linked to an "origin" by HBPVocabulary#INFERENCE_EXTENDS are merged (by applying conflict resolution mechanisms) and provided to the {@link InferenceController} for further processing
 */
@Component
@ToBeTested
public class Reconciliation implements InferenceStrategy, InitializingBean {

    private final static List<String> NAME_BLACKLIST_FOR_MERGE = Arrays.asList(JsonLdConsts.ID, HBPVocabulary.INFERENCE_EXTENDS);

    @Autowired
    InferenceController controller;

    @Autowired
    ArangoRepository repository;

    @Autowired
    ArangoNativeRepository nativeRepository;

    @Autowired
    NexusConfiguration nexusConfiguration;

    @Autowired
    MessageProcessor messageProcessor;

    @Autowired
    NexusToArangoIndexingProvider indexingProvider;

    @Autowired
    JsonTransformer transformer;


    protected Logger logger = LoggerFactory.getLogger(Reconciliation.class);


    @Override
    public void afterPropertiesSet() {
        controller.addInferenceStrategy(this);
    }

    @Override
    public void infer(QualifiedIndexingMessage message, Set<Vertex> documents) {
        //We collect all instances from the default space
        NexusInstanceReference originalId = message.getOriginalId();
        boolean isOriginal = originalId.equals(message.getOriginalMessage().getInstanceReference());
        boolean isSuggestion = message.getOriginalMessage().getInstanceReference().getSubspace().equals(SubSpace.SUGGESTION);

        NexusInstanceReference resolveOriginalId = nativeRepository.findOriginalId(originalId);
        Set<NexusInstanceReference> relativeInstances = indexingProvider.findInstancesWithLinkTo(HBPVocabulary.INFERENCE_EXTENDS, resolveOriginalId);
        Set<NexusInstanceReference> inferredInstances = indexingProvider.findInstancesWithLinkTo(HBPVocabulary.INFERENCE_OF, resolveOriginalId);
        if ( (!isOriginal || (relativeInstances != null && !relativeInstances.isEmpty())) && !isSuggestion) {
            Vertex originalVertex;
            if (isOriginal) {
                originalVertex = messageProcessor.createVertexStructure(message);
            } else {
                originalVertex = indexingProvider.getVertexStructureById(originalId);
            }
            if (originalVertex != null) {
                List<Vertex> relativeStructures = relativeInstances.stream().filter(r -> r != null && !r.getRelativeUrl().equals(message.getOriginalMessage().getInstanceReference().getRelativeUrl())).map(relativeInstance -> indexingProvider.getVertexStructureById(relativeInstance)).collect(Collectors.toList());
                if (!isOriginal) {
                    //Add the new message as part of the relative structure
                    relativeStructures.add(messageProcessor.createVertexStructure(message));
                }
                //Now we apply the inference logic
                Vertex newVertex = mergeInstances(originalVertex, new ArrayList<>(relativeStructures), inferredInstances);
                //And we add the inferred instance to the collection of documents
                documents.add(newVertex);
            }
        }
    }


    private NexusInstanceReference getInstanceReferenceForInferred(NexusInstanceReference original, Set<NexusInstanceReference> inferredInstances) {
        if (inferredInstances != null && !inferredInstances.isEmpty()) {
            if (inferredInstances.size() == 1) {
                return inferredInstances.iterator().next().clone();
            } else {
                throw new InferenceException(String.format("Multiple inferred entities for the original entity %s", original.getFullId(true)));
            }
        } else {
            //There is no inferred instance yet - so we create a new one.
            return new NexusInstanceReference(original.getNexusSchema(), null).toSubSpace(SubSpace.INFERRED);
        }
    }


    private Vertex mergeInstances(Vertex original, List<Vertex> additionalInstances, Set<NexusInstanceReference> inferredInstances) {
        JsonDocument document = new JsonDocument();
        // Setting the original identifier
        document.put(SchemaOrgVocabulary.IDENTIFIER,original.getQualifiedIndexingMessage().getQualifiedMap().get(SchemaOrgVocabulary.IDENTIFIER));
        NexusInstanceReference referenceForInferred = getInstanceReferenceForInferred(original.getInstanceReference(), inferredInstances);
        Set<Vertex> allVertices = new HashSet<>(additionalInstances);
        allVertices.add(original);
        mergeVertex(document, allVertices);
        document.addReference(HBPVocabulary.INFERENCE_OF, nexusConfiguration.getAbsoluteUrl(original.getInstanceReference()));
        document.addType(HBPVocabulary.INFERENCE_TYPE);
        document.put(HBPVocabulary.PROVENANCE_MODIFIED_AT, ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT));
        removeInternalFieldsFromAlternatives(document);
        IndexingMessage indexingMessage = new IndexingMessage(referenceForInferred, transformer.getMapAsJson(document), null, null);
        return messageProcessor.createVertexStructure(messageProcessor.qualify(indexingMessage));
    }

    private void removeInternalFieldsFromAlternatives(JsonDocument doc){
        Map<String, Alternative> alternatives = (Map) doc.get(HBPVocabulary.INFERENCE_ALTERNATIVES);
        if(alternatives != null){
            alternatives.entrySet().stream().filter(k -> !k.getKey().startsWith("@") && !k.getKey().startsWith("_") && !k.getKey().startsWith(HBPVocabulary.PROVENANCE) && !k.getValue().getUserIds().isEmpty());
            doc.put(HBPVocabulary.INFERENCE_ALTERNATIVES, alternatives);
        }
    }

    private Property mergeProperty(String currentProperty, Set<? extends Vertex> vertices) {
        if (!HBPVocabulary.INFERENCE_EXTENDS.equals(currentProperty)) {
            List<Vertex> verticesWithProperty = vertices.stream().filter(v -> v.getQualifiedIndexingMessage().getQualifiedMap().get(currentProperty) != null).collect(Collectors.toList());
            Object result = null;
            Vertex originOfResult = null;
            Map<Object, Set<String>> allAlts = new HashMap<>();
            for (Vertex vertex : verticesWithProperty) {
                Object valueByName = vertex.getQualifiedIndexingMessage().getQualifiedMap().get(currentProperty);
                if(!currentProperty.startsWith("@") && !currentProperty.startsWith("_")){
                    Set<String> userIds = allAlts.get(valueByName);
                    if(userIds == null){
                        userIds = new HashSet<>();
                    }
                    userIds.add((String)vertex.getQualifiedIndexingMessage().getQualifiedMap().get(HBPVocabulary.PROVENANCE_LAST_MODIFICATION_USER_ID));
                    Set<String> cleanIds = userIds.stream().filter(id -> id != null).map(id -> {
                        if(id.contains("/")){
                         return id.substring(id.lastIndexOf("/") + 1);
                    } else { return id;}}).collect(Collectors.toSet());
                    allAlts.put(valueByName, cleanIds);
                }

                if (overrides(vertex, originOfResult, result, currentProperty)) {
                    result = valueByName;
                    originOfResult = vertex;
                }
            }
            final Object r = result;
            Set<Alternative> resultingAlts = new LinkedHashSet<>();
            for(Map.Entry<Object, Set<String>> entry : allAlts.entrySet()){
                Alternative  a = new Alternative(entry.getKey(), entry.getValue(), entry.getKey().equals(r));
                resultingAlts.add(a);
            }
            return new Property(currentProperty, result).setAlternatives(resultingAlts);
        }
        return null;
    }


    void mergeVertex(JsonDocument newDocument, Set<? extends Vertex> vertices) {
        Set<String> handledKeys = new HashSet<>();
        for (Vertex vertex : vertices) {
            for (Object k : vertex.getQualifiedIndexingMessage().getQualifiedMap().keySet()) {
                String key = (String) k;
                if (!handledKeys.contains(key) && !key.equals(HBPVocabulary.INFERENCE_ALTERNATIVES) && !key.equals(SchemaOrgVocabulary.IDENTIFIER) ) {
                    Property property = mergeProperty(key, vertices);
                    if (property != null) {
                        newDocument.put(key, property.getValue());
                        if (property.getAlternatives() != null) {
                            property.getAlternatives().forEach(p -> newDocument.addAlternative(key, p));
                        }
                        handledKeys.add(key);
                    }
                }
            }
        }
    }

    private int compareVertexPower(Vertex newVertex, Vertex oldVertex) {
        SubSpace subspaceNew = newVertex == null ? null : newVertex.getInstanceReference().getSubspace();
        SubSpace subspaceOld = oldVertex == null ? null : oldVertex.getInstanceReference().getSubspace();
        if (subspaceNew == subspaceOld) {
            return 0;
        } else if (SubSpace.EDITOR == subspaceNew) {
            return 1;
        } else {
            return -1;
        }
    }

    private LocalDateTime getModifiedAt(Vertex vertex, String property) {
        Map qualifiedMap = vertex.getQualifiedIndexingMessage().getQualifiedMap();
        Object fieldUpdates = qualifiedMap.get(HBPVocabulary.PROVENANCE_FIELD_UPDATES);
        //Fallback logic since update times got introduced later, we have to check if it's there. If not, we fall back to the modification date of the underlying document.
        if(fieldUpdates instanceof Map && ((Map)fieldUpdates).get(property) instanceof String){
            return LocalDateTime.parse((String)((Map)fieldUpdates).get(property), DateTimeFormatter.ISO_ZONED_DATE_TIME);
        }
        else {
            Object indexedAt = qualifiedMap.get(HBPVocabulary.PROVENANCE_MODIFIED_AT);
            if (indexedAt instanceof String) {
                return LocalDateTime.parse((String) indexedAt, DateTimeFormatter.ISO_ZONED_DATE_TIME);
            }
        }
        return null;
    }


    private boolean overrides(Vertex potentialOverride, Vertex currentVertex, Object currentValue, String property) {
        int i = compareVertexPower(potentialOverride, currentVertex);
        boolean overrides = currentValue == null;
        if (i > 0) {
            overrides = true;
        } else if (i == 0 && !overrides) {
            LocalDateTime indexedAt = getModifiedAt(potentialOverride, property);
            if (currentVertex != null) {
                if (indexedAt != null) {
                    LocalDateTime originIndexedAt = getModifiedAt(currentVertex, property);
                    if (originIndexedAt == null || indexedAt.isAfter(originIndexedAt)) {
                        overrides = true;
                    }
                }
            } else {
                overrides = true;
            }

        }
        return overrides;
    }

}
