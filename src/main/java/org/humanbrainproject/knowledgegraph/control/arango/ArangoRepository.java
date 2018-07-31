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
import java.util.logging.Level;

@Component
public class ArangoRepository extends VertexRepository<ArangoDriver> {

    @Autowired
    ArangoNamingConvention namingConvention;

    @Autowired
    Configuration configuration;

    public String getById(String collectionName, String id, ArangoDriver arango){
        String query = String.format("FOR doc IN `%s` FILTER doc._key==\"%s\" RETURN doc", collectionName, id);
        ArangoCursor<String> q = arango.getOrCreateDB().query(query, null, new AqlQueryOptions(), String.class);
        return q.hasNext() ? q.next() : null;
    }

    public Tuple<String, Integer> countInstances(String collectionName, ArangoDriver arango){
        ArangoCursor<Integer> q = arango.getOrCreateDB().query(String.format("RETURN LENGTH(`%s`)", namingConvention.getVertexLabel(collectionName)), null, new AqlQueryOptions(), Integer.class);
        return q.hasNext() ? new Tuple<>(collectionName, q.next()) : null;
    }


    @Override
    protected Integer getRevisionById(JsonLdVertex vertex, ArangoDriver arango) {
        ArangoDatabase db = arango.getOrCreateDB();
        try{
            String query = String.format("FOR doc IN `%s` FILTER doc._key==\"%s\" RETURN doc.`%s`", namingConvention.getVertexLabel(vertex), namingConvention.getUuid(vertex), configuration.getRev());
            ArangoCursor<Integer> q = db.query(query, null, new AqlQueryOptions(), Integer.class);
            return q.hasNext() ? q.next() : null;
        }
        catch (ArangoDBException e){
            return null;
        }
    }

    @Override
    public void deleteVertex(String entityName, String id, ArangoDriver arango) {
        String vertexLabel = namingConvention.getVertexLabel(entityName);
        String query = String.format("REMOVE \"%s\" IN `%s`", id, vertexLabel);
        ArangoDatabase db = arango.getOrCreateDB();
        if(db.collection(vertexLabel).exists()) {
            db.query(query, null, new AqlQueryOptions(), String.class);
        }else{
            log.log(Level.WARNING, String.format("Tried to delete instance %s in collection %s although the collection doesn't exist. Skip.", id, vertexLabel));
        }
    }

    @Override
    protected void updateVertex(JsonLdVertex vertex, ArangoDriver arango) throws JSONException {
        String query = String.format("REPLACE %s IN `%s`", toJSONString(vertex), namingConvention.getVertexLabel(vertex));
        System.out.println(query);
        arango.getOrCreateDB().query(query, null, new AqlQueryOptions(), Void.class);
    }

    @Override
    protected void insertVertex(JsonLdVertex vertex, ArangoDriver arango) throws JSONException {
        String vertexLabel = namingConvention.getVertexLabel(vertex);
        insertVertexDocument(toJSONString(vertex), vertexLabel, arango);
    }

    public void insertVertexDocument(String jsonLd, String vertexLabel, ArangoDriver arango) throws JSONException {
        ArangoDatabase db = arango.getOrCreateDB();
        ArangoCollection collection = db.collection(vertexLabel);
        if(!collection.exists()) {
            System.out.println(String.format("Create collection %s", vertexLabel));
            db.createCollection(vertexLabel);
        }
        String query = String.format("INSERT %s IN `%s`", jsonLd, vertexLabel);
        System.out.println(query);
        ArangoCursor<String> q = db.query(query, null, new AqlQueryOptions(), String.class);
        while(q.hasNext()){
            String result = q.next();
            System.out.println(result);
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
            System.out.println(String.format("Create edge collection %s", edgeLabel));
            db.createCollection(edgeLabel, collectionCreateOptions);
        }
        JSONObject o = new JSONObject();
        String from = namingConvention.getDocumentHandle(vertex);
        o.put("_from", from);
        String to = namingConvention.getKeyFromReference(edge.getReference());
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
        System.out.println(query);
        db.query(query, null, new AqlQueryOptions(), String.class);
    }



    @Override
    protected boolean hasEdge(JsonLdVertex vertex, JsonLdEdge edge, ArangoDriver arango) throws JSONException {
        ArangoDatabase db = arango.getOrCreateDB();
        String edgeLabel = namingConvention.getEdgeLabel(edge);
        String from = namingConvention.getDocumentHandle(vertex);
        String to = namingConvention.getKeyFromReference(edge.getReference());
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
        System.out.println("Update edge");
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
        o.put("_key", namingConvention.getUuid(vertex));
        o.put("_id", namingConvention.getUuid(vertex));
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
