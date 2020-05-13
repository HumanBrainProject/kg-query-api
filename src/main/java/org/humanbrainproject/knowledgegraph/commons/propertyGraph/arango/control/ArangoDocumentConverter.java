/*
 * Copyright 2020 EPFL/Human Brain Project PCO
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control;

import com.github.jsonldjava.core.JsonLdConsts;
import org.humanbrainproject.knowledgegraph.annotations.Tested;
import org.humanbrainproject.knowledgegraph.commons.jsonld.control.JsonLdStandardization;
import org.humanbrainproject.knowledgegraph.commons.jsonld.control.JsonTransformer;
import org.humanbrainproject.knowledgegraph.commons.nexus.control.NexusConfiguration;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoDocumentReference;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.entity.Edge;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.entity.JsonPath;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.entity.Step;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.entity.Vertex;
import org.humanbrainproject.knowledgegraph.commons.vocabulary.ArangoVocabulary;
import org.humanbrainproject.knowledgegraph.commons.vocabulary.HBPVocabulary;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusInstanceReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Component
@Tested
public class ArangoDocumentConverter {

    @Autowired
    JsonTransformer jsonTransformer;

    @Autowired
    NexusConfiguration configuration;

    @Autowired
    JsonLdStandardization standardization;

    Map<String, Object> buildPath(Step step, List<Step> remaining) {
        Map<String, Object> path = new LinkedHashMap<>();
        if (step != null) {
            path.put(ArangoVocabulary.ORDER_NUMBER, step.getOrderNumber());
            path.put(ArangoVocabulary.NAME, step.getName());
        }
        if (remaining.size() > 0) {
            path.put(ArangoVocabulary.NEXT, buildPath(remaining.get(0), remaining.subList(1, remaining.size())));
        }
        return path;
    }

    public String createJsonFromLinkingInstance(ArangoDocumentReference targetDocument, NexusInstanceReference from, NexusInstanceReference to, NexusInstanceReference mainObject, Vertex vertex){
        Map<String, Object> map = new LinkedHashMap(vertex.getQualifiedIndexingMessage().getQualifiedMap());
        addDefaultDataFromVertex(map, targetDocument, vertex);
        map.put(ArangoVocabulary.FROM, ArangoDocumentReference.fromNexusInstance(from).getId());
        map.put(ArangoVocabulary.TO, ArangoDocumentReference.fromNexusInstance(to).getId());
        map.put(ArangoVocabulary.NAME, mainObject.getNexusSchema().getRelativeUrl().getUrl());
        //This is a loop - it can happen (e.g. for reconciled instances - so we should ensure this never reaches the database).
        if (map.get(ArangoVocabulary.FROM).equals(map.get(ArangoVocabulary.TO))) {
            return null;
        }
        return jsonTransformer.getMapAsJson(map);
    }

    public String createJsonFromEdge(ArangoDocumentReference targetDocument, Vertex vertex, Edge edge, Set<JsonPath> blackList) {
        Map<String, Object> map = new HashMap<>();
        map.put(ArangoVocabulary.ID, targetDocument.getId());
        map.put(ArangoVocabulary.KEY, targetDocument.getKey());
        map.put(ArangoVocabulary.FROM, ArangoDocumentReference.fromNexusInstance(vertex.getInstanceReference()).getId());
        map.put(ArangoVocabulary.TO, ArangoDocumentReference.fromNexusInstance(edge.getReference()).getId());
        map.put(ArangoVocabulary.PATH, buildPath(null, edge.getPath()));
        map.put(ArangoVocabulary.NAME, edge.getName());
        map.put(ArangoVocabulary.ORDER_NUMBER, edge.getLastOrderNumber());
        map.put(ArangoVocabulary.INDEXED_IN_ARANGO_AT, ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT));

        //This is a loop - it can happen (e.g. for reconciled instances - so we should ensure this never reaches the database).
        if (map.get(ArangoVocabulary.FROM).equals(map.get(ArangoVocabulary.TO))) {
            return null;
        }
        for (JsonPath steps : blackList) {
            removePathFromMap(map, steps);
        }
        return jsonTransformer.getMapAsJson(map);
    }

    private void addDefaultDataFromVertex(Map<String, Object> jsonObject, ArangoDocumentReference reference, Vertex vertex){
        jsonObject.put(JsonLdConsts.ID, configuration.getAbsoluteUrl(vertex.getInstanceReference()));
        jsonObject.put(ArangoVocabulary.ID, reference.getId());
        jsonObject.put(ArangoVocabulary.KEY, reference.getKey());
        jsonObject.put(HBPVocabulary.RELATIVE_URL_OF_INTERNAL_LINK, vertex.getInstanceReference().getRelativeUrl().getUrl());
        Integer revision = vertex.getQualifiedIndexingMessage().getOriginalMessage().getInstanceReference().getRevision();
        jsonObject.put(ArangoVocabulary.NEXUS_REV, revision);
        jsonObject.put(ArangoVocabulary.NEXUS_UUID, vertex.getQualifiedIndexingMessage().getOriginalMessage().getInstanceReference().getId());
        jsonObject.put(ArangoVocabulary.NEXUS_RELATIVE_URL, vertex.getQualifiedIndexingMessage().getOriginalMessage().getInstanceReference().getRelativeUrl().getUrl());
        jsonObject.put(ArangoVocabulary.NEXUS_RELATIVE_URL_WITH_REV, vertex.getQualifiedIndexingMessage().getOriginalMessage().getInstanceReference().getFullId(true));
        jsonObject.put(ArangoVocabulary.PERMISSION_GROUP, vertex.getInstanceReference().getNexusSchema().getOrganization());
        jsonObject.put(ArangoVocabulary.INDEXED_IN_ARANGO_AT, ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT));
    }



    public String createJsonFromVertex(ArangoDocumentReference reference, Vertex vertex, Set<JsonPath> blackList) {
        Map<String, Object> jsonObject = new LinkedHashMap(vertex.getQualifiedIndexingMessage().getQualifiedMap());
        addDefaultDataFromVertex(jsonObject, reference, vertex);
        for (JsonPath steps : blackList) {
            removePathFromMap(jsonObject, steps);
        }
        jsonObject = standardization.extendInternalReferencesWithRelativeUrl(jsonObject, null);
        return jsonTransformer.getMapAsJson(jsonObject);
    }

    boolean removePathFromMap(Map map, List<Step> path) {
        if (path.size() == 1) {
            map.remove(path.get(0).getName());
        } else if (path.size() > 1) {
            Step nextStep = path.get(0);
            Object object = map.get(nextStep.getName());
            if (object instanceof Map) {
                boolean fullyRemoved = removePathFromMap(((Map) object), path.subList(1, path.size()));
                if(fullyRemoved){
                    map.remove(nextStep.getName());
                }
            } else if (object instanceof Collection) {
                boolean allRemoved = true;
                for (Object o : ((Collection) object)) {
                    if (o instanceof Map) {
                        boolean fullyRemoved = removePathFromMap(((Map) o), path.subList(1, path.size()));
                        if(!fullyRemoved){
                            allRemoved = false;
                        }
                    }
                }
                if(allRemoved){
                    map.remove(nextStep.getName());
                }
            }
        }
        return map.isEmpty();
    }
}
