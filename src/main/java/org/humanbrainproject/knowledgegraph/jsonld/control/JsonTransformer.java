package org.humanbrainproject.knowledgegraph.jsonld.control;

import com.google.gson.Gson;
import deprecated.control.arango.ArangoNamingConvention;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class JsonTransformer {

    Gson gson = new Gson();

    protected Logger logger = LoggerFactory.getLogger(ArangoNamingConvention.class);

    @Autowired
    ArangoNamingConvention namingConvention;

    public String getMapAsJson(Map map){
        return gson.toJson(map);
    }


    public Map parseToMap(String json) {
        return gson.fromJson(json, Map.class);
    }
//
//    Object resolveProperty(Object property) {
//        if(property instanceof JsonLdProperty){
//            JsonLdProperty jsonld = (JsonLdProperty)property;
//            Map result = new LinkedHashMap();
//            if(jsonld.getName()!=null && jsonld.getValue()!=null) {
//                result.put(jsonld.getName(), resolveProperty(jsonld.getValue()));
//            }
//            return result;
//        }
//        else if(property instanceof Collection){
//            List array = new ArrayList();
//            for (Object o : ((Collection) property)) {
//                Object result = resolveProperty(o);
//                if(result!=null){
//                    array.add(result);
//                }
//            }
//            return array;
//        }
//        else {
//            return property;
//        }
//    }
//
//
//    Map recreateObjectFromProperties(Set<JsonLdProperty> properties) throws JSONException {
//        Map o = new LinkedHashMap();
//        for (JsonLdProperty jsonLdProperty : properties) {
//            if (jsonLdProperty.getName() != null) {
//                o.put(jsonLdProperty.getName(), resolveProperty(jsonLdProperty.getValue()));
//            }
//        }
//        return o;
//    }
//
//    public String vertexToJSONString(JsonLdVertex vertex, boolean removeId) {
//        try {
//            Map o = recreateObjectFromProperties(vertex.getProperties());
//            o.put("_key", namingConvention.getKey(vertex));
//            if (!vertex.isEmbedded() && !removeId) {
//                o.put(JsonLdConsts.ID, String.format("%s/%s", vertex.getEntityName(), namingConvention.getKey(vertex)));
//            }
//            return new Gson().toJson(o);
//        } catch (JSONException e) {
//            logger.error("Was not able to translate Vertex into JSON", e);
//            return null;
//        }
//    }
//
//
//    public String edgeToJSONString(JsonLdVertex vertex, JsonLdEdge edge) {
//        try {
//            JSONObject o = new JSONObject();
//
//            String from = namingConvention.getId(vertex);
//            o.put("_from", from);
//            String to = namingConvention.getEdgeTarget(edge);
//            if (to != null) {
//                o.put("_to", to);
//                String key = namingConvention.getReferenceKey(vertex, edge);
//                o.put("_key", key);
//                if (edge.getOrderNumber() != null && edge.getOrderNumber() >= 0) {
//                    o.put("orderNumber", edge.getOrderNumber());
//                }
//                for (JsonLdProperty jsonLdProperty : edge.getProperties()) {
//                    o.put(jsonLdProperty.getName(), jsonLdProperty.getValue());
//                }
//                o.put(JsonLdConsts.ID, null);
//                return o.toString();
//            } else {
//                return null;
//            }
//        } catch (JSONException e) {
//            logger.error("Was not able to translate Vertex into JSON", e);
//            return null;
//        }
//    }


//    private void rebuildEmbeddedDocumentFromEdges(JsonLdVertex vertex) throws JSONException {
//        Map<String, List<JsonLdEdge>> groupedEdges = new HashMap<>();
//        for (JsonLdEdge jsonLdEdge : vertex.getEdges()) {
//            if (!groupedEdges.containsKey(jsonLdEdge.getPostFix())) {
//                groupedEdges.put(jsonLdEdge.getPostFix(), new ArrayList<>());
//            }
//            groupedEdges.get(jsonLdEdge.getPostFix()).add(jsonLdEdge);
//        }
//        Comparator<JsonLdEdge> c = (o1, o2) -> {
//            if (o1 == null || o1.getOrderNumber() == null) {
//                return o2 == null || o2.getOrderNumber() == null ? 0 : -1;
//            } else {
//                return o2 == null || o2.getOrderNumber() == null ? 1 : o1.getOrderNumber().compareTo(o2.getOrderNumber());
//            }
//        };
//        groupedEdges.values().forEach(l -> l.sort(c));
//        for (String name : groupedEdges.keySet()) {
//            JsonLdProperty p = new JsonLdProperty();
//            p.setName(name);
//            List<JsonLdEdge> jsonLdEdges = groupedEdges.get(name);
//            List<JSONObject> nested = new ArrayList<>();
//            for (JsonLdEdge jsonLdEdge : jsonLdEdges) {
//                nested.add(recreateObjectFromProperties(jsonLdEdge.getProperties()));
//            }
//            p.setValue(nested.size() == 1 ? nested.get(0) : nested);
//            vertex.getProperties().add(p);
//        }
//    }

}
