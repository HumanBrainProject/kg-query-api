package org.humanbrainproject.knowledgegraph.control.jsonld;


import com.github.jsonldjava.core.JsonLdConsts;
import org.humanbrainproject.knowledgegraph.control.Configuration;
import org.humanbrainproject.knowledgegraph.control.SubSpaceName;
import org.humanbrainproject.knowledgegraph.entity.indexing.GraphIndexingSpec;
import org.humanbrainproject.knowledgegraph.entity.jsonld.JsonLdEdge;
import org.humanbrainproject.knowledgegraph.entity.jsonld.JsonLdProperty;
import org.humanbrainproject.knowledgegraph.entity.jsonld.JsonLdVertex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * This class provides the tools to transform a (previously harmonized {@see org.humanbrainproject.propertygraph.jsonld.control.jsonld.JsonLdStandardization})
 * JSON-LD structure to a data structure of vertices and edges which is understood by property graphs.
 */
@Component
public class JsonLdToVerticesAndEdges {

    @Autowired
    Configuration configuration;

    /**
     * Takes a jsonLdPayload (fully qualified) and transforms it into a list of vertices including their outgoing edges and prepared properties.
     *
     */
    public List<JsonLdVertex> transformFullyQualifiedJsonLdToVerticesAndEdges(GraphIndexingSpec spec, Map map, boolean translateToMainSpace) {
        if(translateToMainSpace){
            spec.setEntityName(translateReferenceToMainspace(spec.getEntityName()));
        }
        return createVertex(null, map, null, new ArrayList<>(), -1, spec.getEntityName(), spec.getPermissionGroup(), spec.getId(), spec.getRevision(), translateToMainSpace);
    }


    String translateReferenceToMainspace(String originalReference){
        if(originalReference!=null){
            String result = originalReference;
            for (SubSpaceName subSpaceName : SubSpaceName.values()) {
                String format = String.format("%s(?=/.*?/.*?/v\\d*\\.\\d*\\.\\d*)", subSpaceName.getName());
                result = result.replaceAll(format, "");
            }
            return result;
        }
        return null;
    }


    private JsonLdEdge createEdge(Map data, JsonLdVertex linkedVertex, String name, boolean translateToMainSpace) {
        JsonLdEdge edge = new JsonLdEdge();
        edge.setName(name);
        if (data.containsKey(JsonLdConsts.ID)) {
            //The json contains a "@id" reference -> it's linking to something "outside" of the document, so we store the reference.
            String reference = data.get(JsonLdConsts.ID).toString();
            if(translateToMainSpace){
                reference = translateReferenceToMainspace(reference);
                data.put(JsonLdConsts.ID, reference);
            }
            edge.setReference(reference);
        } else {
            //An edge shall be created without an explicit "@id". This means it is a nested object. We therefore save the linked vertex as well.
            edge.setTarget(linkedVertex);
        }
        //Append properties on the relation to the edge
        for (Object key : data.keySet()) {
            edge.getProperties().add(createJsonLdProperty(key.toString(), data.get(key)));
        }
        return edge;
    }


    private List<JsonLdVertex> createVertex(String key, Object data, JsonLdVertex parent, List<JsonLdVertex> vertexCollection, int orderNumber, String entityName, String permissionGroup, String id, Integer revision, boolean translateToMainSpace) {
        if(data instanceof Map){
            Map map = (Map)data;
            if(map.size()==0){
                //Skip empty objects.
                return vertexCollection;
            }
            JsonLdVertex v = new JsonLdVertex();
            updateId(key, parent, v, entityName, id, orderNumber);
            updateRevision(parent, v, revision);
            updatePermissionGroup(v, permissionGroup);
            if(map.containsKey(JsonLdConsts.VALUE)){
                return createVertex(key, map.get(JsonLdConsts.VALUE), parent, vertexCollection, orderNumber, v.getEntityName(), permissionGroup, id, revision, translateToMainSpace);
            }
            if(map.containsKey(configuration.getEmbedded()) && map.get(configuration.getEmbedded()) instanceof Boolean){
                v.setEmbedded((Boolean)map.get(configuration.getEmbedded()));
            }

            if (handleOrderedList(key, parent, vertexCollection, map, v.getEntityName(), permissionGroup, id, revision, translateToMainSpace)) {
                //Since it's an ordered list, we already took care of its sub elements and can cancel this branch of recursive execution
                return vertexCollection;
            }
            JsonLdEdge edgeForVertex = createEdgesForVertex(key, parent, orderNumber, map, v, translateToMainSpace);
            updateEmbedded(v);
            if (edgeForVertex!=null && edgeForVertex.isExternal()){
                //Since it is an external connection, we can stop here
                return vertexCollection;
            }
            vertexCollection.add(v);
            for (Object k : map.keySet()) {
                createVertex(k.toString(), map.get(k), v, vertexCollection, -1, k.toString(), permissionGroup, id, revision, translateToMainSpace);
            }
        } else if (data instanceof List) {
            List list = (List) data;
            for (Object i : list) {
                createVertex(key, i, parent, vertexCollection, -1, key, id, permissionGroup, revision, translateToMainSpace);
            }
        } else if (data!=null){
            //It's a leaf node - add it as a property
            parent.getProperties().add(createJsonLdProperty(key, data));
        }
        return vertexCollection;
    }

    private JsonLdEdge createEdgesForVertex(String key, JsonLdVertex parent, int orderNumber, Map data, JsonLdVertex v, boolean translateToMainSpace) {
        if (parent != null) {
            JsonLdEdge edge = createEdge(data, v, key, translateToMainSpace);
            v.setEmbedded(edge.isEmbedded());
            parent.getEdges().add(edge);
            if(!edge.isEmbedded() && edge.getReference()!=null){
                JsonLdProperty property = parent.getPropertyByName(key);
                if(property==null){
                    property = new JsonLdProperty();
                    property.setName(key);
                    parent.addProperty(property);
                }
                JsonLdProperty reference = new JsonLdProperty();
                reference.setName(JsonLdConsts.ID);
                reference.setValue(edge.getReference());
                if(property.getValue()==null){
                    property.setValue(reference);
                }
                else{
                    if(!(property.getValue() instanceof Collection)){
                        List<Object> values = new ArrayList<>();
                        values.add(property.getValue());
                        property.setValue(values);
                    }
                    ((Collection<Object>)property.getValue()).add(reference);
                }
            }
            if (orderNumber >= 0) {
                edge.setOrderNumber(orderNumber);
            }
            return edge;
        }
        return null;
    }


    private boolean handleOrderedList(String key, JsonLdVertex parent, List<JsonLdVertex> vertexCollection, Map data, String entityName, String permissionGroup, String id, Integer revision, boolean translateToMainSpace) {
        if (data.containsKey(JsonLdConsts.LIST) && data.get(JsonLdConsts.LIST) instanceof List) {
            List list = (List) data.get(JsonLdConsts.LIST);
            for (int i = 0; i < list.size(); i++) {
                if(list.get(i) instanceof Map) {
                    createVertex(key, (Map) list.get(i), parent, vertexCollection, i, entityName, id, permissionGroup, revision, translateToMainSpace);
                }
            }
            return true;
        }
        return false;
    }


    private JsonLdProperty createJsonLdProperty(String key, Object value) {
        JsonLdProperty property = new JsonLdProperty();
        property.setName(key);
        property.setValue(value);
        return property;
    }

    private void updateRevision(JsonLdVertex parent, JsonLdVertex v, Integer revision) {
        if (parent==null) {
            v.setRevision(revision);
        } else if (parent.getRevision() != null) {
            v.setRevision(parent.getRevision());
        }
        JsonLdProperty p = new JsonLdProperty();
        p.setName(configuration.getRev());
        p.setValue(v.getRevision());
        v.getProperties().add(p);
    }

    private void updatePermissionGroup(JsonLdVertex vertex, String permissionGroup) {
        JsonLdProperty p = new JsonLdProperty();
        p.setName(configuration.getPermissionGroup());
        p.setValue(permissionGroup);
        vertex.getProperties().add(p);
    }


    private void updateEmbedded(JsonLdVertex vertex) {
        if(vertex.isEmbedded()) {
            JsonLdProperty p = new JsonLdProperty();
            p.setName(configuration.getEmbedded());
            p.setValue(true);
            vertex.getProperties().add(p);
        }
    }

    private void updateId(String key, JsonLdVertex parent, JsonLdVertex v, String entityName, String rootId, int ordernumber) {
        if (parent==null) {
            v.setEntityName(entityName);
            v.setKey(rootId);
        } else if (parent.getKey() != null && key != null) {
            v.setEntityName(key);
            v.setKey(buildEmbeddedId(key, parent.getKey(), ordernumber));
            JsonLdProperty p = new JsonLdProperty();
            p.setName(JsonLdConsts.ID);
            p.setValue(v.getKey());
            v.getProperties().add(p);
        }
    }

    private String buildEmbeddedId(String key, String root, int ordernumber){
        return String.format("%s#%s-%d", key, root, ordernumber);
    }

}
