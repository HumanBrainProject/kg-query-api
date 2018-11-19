package org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control;

import com.github.jsonldjava.core.JsonLdConsts;
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

import java.util.*;

@Component
public class ArangoDocumentConverter {

    @Autowired
    JsonTransformer jsonTransformer;

    @Autowired
    NexusConfiguration configuration;

    @Autowired
    JsonLdStandardization standardization;

    private Map<String, Object> buildPath(Step step, List<Step> remaining) {
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

    public String createJsonFromLinkingInstance(ArangoDocumentReference targetDocument, NexusInstanceReference from, NexusInstanceReference to, NexusInstanceReference mainObject){
        Map<String, Object> map = new HashMap<>();
        map.put(ArangoVocabulary.ID, targetDocument.getId());
        map.put(ArangoVocabulary.KEY, targetDocument.getKey());
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
        //This is a loop - it can happen (e.g. for reconciled instances - so we should ensure this never reaches the database).
        if (map.get(ArangoVocabulary.FROM).equals(map.get(ArangoVocabulary.TO))) {
            return null;
        }
        for (JsonPath steps : blackList) {
            removePathFromMap(map, steps);
        }
        return jsonTransformer.getMapAsJson(map);
    }


    public String createJsonFromVertex(ArangoDocumentReference reference, Vertex vertex, Set<JsonPath> blackList) {
        Map<String, Object> jsonObject = new LinkedHashMap(vertex.getQualifiedIndexingMessage().getQualifiedMap());
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
        for (JsonPath steps : blackList) {
            removePathFromMap(jsonObject, steps);
        }
        jsonObject = standardization.extendInternalReferencesWithRelativeUrl(jsonObject, null);
        return jsonTransformer.getMapAsJson(jsonObject);
    }

    private void removePathFromMap(Map map, List<Step> path) {
        if (path.size() == 1) {
            map.remove(path.get(0).getName());
        } else if (path.size() > 1) {
            Object nextStep = map.get(0);
            if (nextStep instanceof Map) {
                removePathFromMap(((Map) nextStep), path.subList(1, path.size()));
            } else if (nextStep instanceof Collection) {
                for (Object o : ((Collection) nextStep)) {
                    if (o instanceof Map) {
                        removePathFromMap(((Map) o), path.subList(1, path.size()));
                    }
                }
            }

        }


    }
}
