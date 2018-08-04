package org.humanbrainproject.knowledgegraph.control.arango;

import com.arangodb.ArangoCollection;
import com.arangodb.ArangoCursor;
import com.arangodb.ArangoDBException;
import com.arangodb.ArangoDatabase;
import com.arangodb.entity.CollectionType;
import com.arangodb.model.AqlQueryOptions;
import com.arangodb.model.CollectionCreateOptions;
import org.humanbrainproject.knowledgegraph.control.Configuration;
import org.humanbrainproject.knowledgegraph.entity.Tuple;
import org.humanbrainproject.knowledgegraph.entity.jsonld.JsonLdEdge;
import org.humanbrainproject.knowledgegraph.entity.jsonld.JsonLdProperty;
import org.humanbrainproject.knowledgegraph.entity.jsonld.JsonLdVertex;
import org.humanbrainproject.knowledgegraph.control.VertexRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.configurationprocessor.json.JSONException;
import org.springframework.boot.configurationprocessor.json.JSONObject;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class ArangoRepository extends VertexRepository<ArangoDriver> {

    @Autowired
    ArangoNamingConvention namingConvention;

    @Autowired
    Configuration configuration;


    private static final String NAME_LOOKUP_MAP = "name_lookup";

    public String getById(String collectionName, String id, ArangoDriver arango){
        String vertexLabel = namingConvention.getVertexLabel(collectionName);
        String query = String.format("FOR doc IN `%s` FILTER doc._key==\"%s\" RETURN doc", vertexLabel, id);
        ArangoCursor<String> q = arango.getOrCreateDB().query(query, null, new AqlQueryOptions(), String.class);
        return q.hasNext() ? q.next() : null;
    }

    public Tuple<String, Long> countInstances(String collectionName, ArangoDriver arango){
        Long count = arango.getOrCreateDB().collection(namingConvention.getVertexLabel(collectionName)).count().getCount();
        return new Tuple<>(collectionName, count);
    }


    @Override
    protected Long getRevisionById(JsonLdVertex vertex, ArangoDriver arango) {
        ArangoDatabase db = arango.getOrCreateDB();
        try{
            ArangoCollection collection = db.collection(namingConvention.getVertexLabel(vertex.getEntityName()));
            if(collection!=null){
                Map document = collection.getDocument(namingConvention.getKey(vertex), Map.class);
                if(document!=null){
                    return (Long)document.get(configuration.getRev());
                }
            }
            return null;
        }
        catch (ArangoDBException e){
            return null;
        }
    }


    public Map<String, String> getArangoNameMapping(ArangoDatabase db){
        String query = String.format("FOR doc IN `%s` RETURN {\"arango\": doc._key, \"original\": doc.orginalName}", NAME_LOOKUP_MAP);
        ArangoCursor<Map> q = db.query(query, null, new AqlQueryOptions(), Map.class);
        List<Map> instances = q.asListRemaining();
        return instances.stream().collect(HashMap::new, (map, item) -> map.put((String)item.get("arango"), (String)item.get("original")), Map::putAll);
   }

    @Override
    public void deleteVertex(String entityName, String id, ArangoDriver arango) {
        String vertexLabel = namingConvention.getVertexLabel(entityName);
        //String query = String.format("REMOVE \"%s\" IN `%s`", id, vertexLabel);
        ArangoDatabase db = arango.getOrCreateDB();
        ArangoCollection collection = db.collection(vertexLabel);
        if(collection.exists() && collection.documentExists(id)) {
            collection.deleteDocument(id);
            //  db.query(query, null, new AqlQueryOptions(), String.class);
            if(collection.count().getCount()==0){
                collection.drop();
                db.collection(NAME_LOOKUP_MAP).deleteDocument(vertexLabel);
            }
        } else {
            logger.warn("Tried to delete instance {} in collection {} although the collection doesn't exist. Skip.", id, vertexLabel);
        }
    }


    public Map<String, Object> getPropertyCount(String collectionName, ArangoDatabase db){
        String query = String.format("LET attributesPerDocument = ( FOR doc IN `%s` RETURN ATTRIBUTES(doc, true) )\n" +
                "FOR attributeArray IN attributesPerDocument\n" +
                "    FOR attribute IN attributeArray\n" +
                "        COLLECT attr = attribute WITH COUNT INTO count\n" +
                "        SORT count DESC\n" +
                "        RETURN {attr, count}", collectionName);
        ArangoCursor<Map> result = db.query(query, null, new AqlQueryOptions(), Map.class);
        return result.asListRemaining().stream().sorted(Comparator.comparing(a -> ((String) a.get("attr")))).collect(LinkedHashMap::new, (map, item) -> map.put((String)item.get("attr"), (Long)item.get("count")), Map::putAll);
    }


    @Override
    protected void updateVertex(JsonLdVertex vertex, ArangoDriver arango) throws JSONException {
        String query = String.format("REPLACE %s IN `%s`", toJSONString(vertex), namingConvention.getVertexLabel(vertex.getEntityName()));
        arango.getOrCreateDB().query(query, null, new AqlQueryOptions(), Void.class);
    }

    @Override
    protected void insertVertex(JsonLdVertex vertex, ArangoDriver arango) throws JSONException {
        insertVertexDocument(toJSONString(vertex), vertex.getEntityName(), arango);
    }

    public void insertVertexDocument(String jsonLd, String vertexName, ArangoDriver arango) throws JSONException {
        String originalName = vertexName;
        String vertexLabel = namingConvention.getVertexLabel(vertexName);
        ArangoDatabase db = arango.getOrCreateDB();
        ArangoCollection collection = db.collection(vertexLabel);
        if(!collection.exists()) {
            logger.info("Create collection {}", vertexLabel);
            db.createCollection(vertexLabel);
            if(!vertexName.equals(NAME_LOOKUP_MAP)) {
                insertVertexDocument(String.format("{\"orginalName\": \"%s\", \"_key\": \"%s\"}", originalName, vertexLabel), NAME_LOOKUP_MAP, arango);
            }
        }
        String query = String.format("INSERT %s IN `%s`", jsonLd, vertexLabel);
        logger.debug(query);
        ArangoCursor<String> q = db.query(query, null, new AqlQueryOptions(), String.class);
        while(q.hasNext()){
            String result = q.next();
            logger.debug(result);
        }
    }


    @Override
    protected void createEdge(JsonLdVertex vertex, JsonLdEdge edge, int orderNumber, ArangoDriver arango) throws JSONException {
        ArangoDatabase db = arango.getOrCreateDB();
        String edgeLabel = namingConvention.getEdgeLabel(edge);
        ArangoCollection collection = db.collection(edgeLabel);
        if(!collection.exists()){
            CollectionCreateOptions collectionCreateOptions = new CollectionCreateOptions();
            collectionCreateOptions.type(CollectionType.EDGES);
            logger.info("Create edge collection {}", edgeLabel);
            db.createCollection(edgeLabel, collectionCreateOptions);
        }
        JSONObject o = new JSONObject();
        String from = namingConvention.getDocumentHandle(vertex);
        o.put("_from", from);
        String to;
        if(edge.isEmbedded() && edge.getTarget()!=null){
            to = namingConvention.getDocumentHandle(edge.getTarget());
        }
        else if(!edge.isEmbedded() && edge.getReference()!=null){
            to = namingConvention.getKeyFromReference(edge.getReference(), edge.isEmbedded());
        }
        else{
            to = null;
        }
        o.put("_to", to);
        String key = namingConvention.getReferenceKey(from, to);
        o.put("_key", key);
        if(orderNumber>=0){
            o.put("orderNumber", orderNumber);
        }
        for (JsonLdProperty jsonLdProperty : edge.getProperties()) {
            o.put(jsonLdProperty.getName(), jsonLdProperty.getValue());
        }
        String query = String.format("INSERT %s IN `%s`", o.toString(), edgeLabel);
        logger.debug(query);
        db.query(query, null, new AqlQueryOptions(), String.class);
    }



    @Override
    protected boolean hasEdge(JsonLdVertex vertex, JsonLdEdge edge, ArangoDriver arango) throws JSONException {
        ArangoDatabase db = arango.getOrCreateDB();
        String edgeLabel = namingConvention.getEdgeLabel(edge);
        String from = namingConvention.getDocumentHandle(vertex);
        String to = namingConvention.getKeyFromReference(edge.getReference(), edge.isEmbedded());
        try{
            String query = String.format("FOR rel IN `%s` FILTER rel._from==\"%s\" AND rel._to==\"%s\" RETURN rel", edgeLabel, from, to);
            ArangoCursor<String> q = db.query(query, null, new AqlQueryOptions(), String.class);
            return q.hasNext();
        }
        catch (ArangoDBException e){
            return false;
        }
    }

    @Override
    protected void updateEdge(JsonLdVertex vertex, JsonLdEdge edge, int orderNumber, ArangoDriver arango) throws JSONException {
        logger.info("Update edge");
    }

    @Override
    public void updateUnresolved(JsonLdVertex vertex, ArangoDriver arango) {

    }


    private JSONObject recreateObjectFromProperties(Set<JsonLdProperty> properties) throws JSONException {
        JSONObject o = new JSONObject();
        for (JsonLdProperty jsonLdProperty : properties) {
            o.put(jsonLdProperty.getName(), jsonLdProperty.getValue());
        }
        return o;
    }

    private String toJSONString(JsonLdVertex vertex) throws JSONException {
        rebuildEmbeddedDocumentFromEdges(vertex);
        JSONObject o = recreateObjectFromProperties(vertex.getProperties());
        o.put("_key", namingConvention.getKey(vertex));
        o.put("_id", namingConvention.getKey(vertex));
        return o.toString(4);
    }

    private void rebuildEmbeddedDocumentFromEdges(JsonLdVertex vertex) throws JSONException {
        Map<String, List<JsonLdEdge>> groupedEdges = new HashMap<>();
        for (JsonLdEdge jsonLdEdge : vertex.getEdges()) {
            if(!groupedEdges.containsKey(jsonLdEdge.getName())){
                groupedEdges.put(jsonLdEdge.getName(), new ArrayList<>());
            }
            groupedEdges.get(jsonLdEdge.getName()).add(jsonLdEdge);
        }
        Comparator<JsonLdEdge> c = (o1, o2) -> {
            if(o1==null || o1.getOrderNumber()==null){
                return o2==null || o2.getOrderNumber() == null ? 0 : -1;
            }
            else{
                return o2==null || o2.getOrderNumber() == null ? 1 : o1.getOrderNumber().compareTo(o2.getOrderNumber());
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
            p.setValue(nested.size()==1 ? nested.get(0) : nested);
            vertex.getProperties().add(p);
        }
    }

}
