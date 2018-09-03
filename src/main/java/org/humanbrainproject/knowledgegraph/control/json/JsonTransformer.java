package org.humanbrainproject.knowledgegraph.control.json;

import com.github.jsonldjava.core.JsonLdConsts;
import com.google.gson.Gson;
import org.humanbrainproject.knowledgegraph.control.arango.ArangoNamingConvention;
import org.humanbrainproject.knowledgegraph.entity.jsonld.JsonLdEdge;
import org.humanbrainproject.knowledgegraph.entity.jsonld.JsonLdProperty;
import org.humanbrainproject.knowledgegraph.entity.jsonld.JsonLdVertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.configurationprocessor.json.JSONArray;
import org.springframework.boot.configurationprocessor.json.JSONException;
import org.springframework.boot.configurationprocessor.json.JSONObject;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class JsonTransformer {

    Gson gson = new Gson();

    protected Logger logger = LoggerFactory.getLogger(ArangoNamingConvention.class);

    @Autowired
    ArangoNamingConvention namingConvention;

    public Map parseToMap(String json) {
        return gson.fromJson(json, Map.class);
    }

    private JSONObject recreateObjectFromProperties(Set<JsonLdProperty> properties) throws JSONException {
        JSONObject o = new JSONObject();
        for (JsonLdProperty jsonLdProperty : properties) {
            if (jsonLdProperty.getName() != null) {
                if (jsonLdProperty.getValue() instanceof JsonLdProperty) {
                    JsonLdProperty nestedProperty = (JsonLdProperty) jsonLdProperty.getValue();
                    JSONObject o2 = new JSONObject();
                    o2.put(nestedProperty.getName(), nestedProperty.getValue());
                    o.put(jsonLdProperty.getName(), o2);
                } else if (jsonLdProperty.getValue() instanceof Collection) {
                    JSONArray array = new JSONArray();
                    for (Object child : ((Collection) jsonLdProperty.getValue())) {
                        if (child instanceof JsonLdProperty) {
                            JsonLdProperty nestedProperty = (JsonLdProperty) child;
                            JSONObject o2 = new JSONObject();
                            o2.put(nestedProperty.getName(), nestedProperty.getValue());
                            array.put(o2);
                        } else {
                            array.put(child);
                        }
                    }
                    o.put(jsonLdProperty.getName(), array);
                } else {
                    o.put(jsonLdProperty.getName(), jsonLdProperty.getValue());
                }
            }
        }
        return o;
    }

    public String vertexToJSONString(JsonLdVertex vertex) {
        try {
            JSONObject o = recreateObjectFromProperties(vertex.getProperties());
            o.put("_key", namingConvention.getKey(vertex));
            if (!vertex.isEmbedded()) {
                o.put(JsonLdConsts.ID, String.format("%s/%s", vertex.getEntityName(), namingConvention.getKey(vertex)));
            }
            return o.toString();
        } catch (JSONException e) {
            logger.error("Was not able to translate Vertex into JSON", e);
            return null;
        }
    }


    public String edgeToJSONString(JsonLdVertex vertex, JsonLdEdge edge) {
        try {
            JSONObject o = new JSONObject();

            String from = namingConvention.getId(vertex);
            o.put("_from", from);
            String to = namingConvention.getEdgeTarget(edge);
            if (to != null) {
                o.put("_to", to);
                String key = namingConvention.getReferenceKey(vertex, edge);
                o.put("_key", key);
                if (edge.getOrderNumber() != null && edge.getOrderNumber() >= 0) {
                    o.put("orderNumber", edge.getOrderNumber());
                }
                for (JsonLdProperty jsonLdProperty : edge.getProperties()) {
                    o.put(jsonLdProperty.getName(), jsonLdProperty.getValue());
                }
                o.put(JsonLdConsts.ID, null);
                return o.toString();
            } else {
                return null;
            }
        } catch (JSONException e) {
            logger.error("Was not able to translate Vertex into JSON", e);
            return null;
        }
    }


    private void rebuildEmbeddedDocumentFromEdges(JsonLdVertex vertex) throws JSONException {
        Map<String, List<JsonLdEdge>> groupedEdges = new HashMap<>();
        for (JsonLdEdge jsonLdEdge : vertex.getEdges()) {
            if (!groupedEdges.containsKey(jsonLdEdge.getName())) {
                groupedEdges.put(jsonLdEdge.getName(), new ArrayList<>());
            }
            groupedEdges.get(jsonLdEdge.getName()).add(jsonLdEdge);
        }
        Comparator<JsonLdEdge> c = (o1, o2) -> {
            if (o1 == null || o1.getOrderNumber() == null) {
                return o2 == null || o2.getOrderNumber() == null ? 0 : -1;
            } else {
                return o2 == null || o2.getOrderNumber() == null ? 1 : o1.getOrderNumber().compareTo(o2.getOrderNumber());
            }
        };
        groupedEdges.values().forEach(l -> l.sort(c));
        for (String name : groupedEdges.keySet()) {
            JsonLdProperty p = new JsonLdProperty();
            p.setName(name);
            List<JsonLdEdge> jsonLdEdges = groupedEdges.get(name);
            List<JSONObject> nested = new ArrayList<>();
            for (JsonLdEdge jsonLdEdge : jsonLdEdges) {
                nested.add(recreateObjectFromProperties(jsonLdEdge.getProperties()));
            }
            p.setValue(nested.size() == 1 ? nested.get(0) : nested);
            vertex.getProperties().add(p);
        }
    }

}
