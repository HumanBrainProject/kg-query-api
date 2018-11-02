package org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control;

import org.humanbrainproject.knowledgegraph.commons.jsonld.control.JsonTransformer;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.query.ArangoSpecificationQuery;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoDocumentReference;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.entity.EdgeX;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.entity.JsonPath;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.entity.Step;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.entity.Vertex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class ArangoDocumentConverter {

    @Autowired
    JsonTransformer jsonTransformer;


    private Map<String, Object> buildPath(Step step, List<Step> remaining) {
        Map<String, Object> path = new LinkedHashMap<>();
        if (step != null) {
            path.put("_orderNumber", step.getOrderNumber());
            path.put("_name", step.getName());
        }
        if (remaining.size() > 0) {
            path.put("_next", buildPath(remaining.get(0), remaining.subList(1, remaining.size())));
        }
        return path;
    }

    public String createJsonFromEdge(Vertex vertex, EdgeX edge, Set<JsonPath> blackList) {
        Map<String, Object> map = new HashMap<>();
        map.put("_from", ArangoDocumentReference.fromNexusInstance(vertex.getInstanceReference()).getId());
        map.put("_to", ArangoDocumentReference.fromNexusInstance(edge.getReference()).getId());
        map.put("_path", buildPath(null, edge.getPath()));
        map.put("_name", edge.getName());
        //This is a loop - it can happen (e.g. for reconciled instances - so we should ensure this never reaches the database).
        if (map.get("_from").equals(map.get("_to"))) {
            return null;
        }
        for (JsonPath steps : blackList) {
            removePathFromMap(map, steps);
        }
        return jsonTransformer.getMapAsJson(map);
    }


    public String createJsonFromVertex(ArangoDocumentReference reference, Vertex vertex, Set<JsonPath> blackList) {
        Map<String, Object> jsonObject = new LinkedHashMap(vertex.getQualifiedIndexingMessage().getQualifiedMap());
        jsonObject.put("_id", reference.getId());
        jsonObject.put("_key", reference.getKey());
        jsonObject.put("_originalId", vertex.getQualifiedIndexingMessage().getOriginalMessage().getInstanceReference().getFullId(true));
        jsonObject.put(ArangoSpecificationQuery.PERMISSION_GROUP, vertex.getInstanceReference().getNexusSchema().getOrganization());
        for (JsonPath steps : blackList) {
            removePathFromMap(jsonObject, steps);
        }
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
