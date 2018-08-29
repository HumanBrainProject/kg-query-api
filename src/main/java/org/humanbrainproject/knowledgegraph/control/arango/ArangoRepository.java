package org.humanbrainproject.knowledgegraph.control.arango;

import com.arangodb.ArangoCollection;
import com.arangodb.ArangoCursor;
import com.arangodb.ArangoDBException;
import com.arangodb.ArangoDatabase;
import com.arangodb.entity.CollectionEntity;
import com.arangodb.entity.CollectionType;
import com.arangodb.model.AqlQueryOptions;
import com.arangodb.model.CollectionCreateOptions;
import com.github.jsonldjava.core.JsonLdConsts;
import org.humanbrainproject.knowledgegraph.control.Configuration;
import org.humanbrainproject.knowledgegraph.control.VertexRepository;
import org.humanbrainproject.knowledgegraph.entity.Tuple;
import org.humanbrainproject.knowledgegraph.entity.jsonld.JsonLdEdge;
import org.humanbrainproject.knowledgegraph.entity.jsonld.JsonLdProperty;
import org.humanbrainproject.knowledgegraph.entity.jsonld.JsonLdVertex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.configurationprocessor.json.JSONArray;
import org.springframework.boot.configurationprocessor.json.JSONException;
import org.springframework.boot.configurationprocessor.json.JSONObject;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class ArangoRepository extends VertexRepository<ArangoDriver> {

    @Autowired
    ArangoNamingConvention namingConvention;

    @Autowired
    Configuration configuration;

    @Autowired
    ArangoQueryFactory queryFactory;


    private static final String NAME_LOOKUP_MAP = "name_lookup";

    public <T> T getByKey(String collectionName, String key, Class<T> clazz, ArangoDriver arango) {
        String vertexLabel = namingConvention.getVertexLabel(collectionName);
        return arango.getOrCreateDB().collection(vertexLabel).getDocument(key, clazz);
    }

    public Tuple<String, Long> countInstances(String collectionName, ArangoDriver arango) {
        Long count = arango.getOrCreateDB().collection(namingConvention.getVertexLabel(collectionName)).count().getCount();
        return new Tuple<>(collectionName, count);
    }

    public void stageElementsToReleased(Set<String> vertexIds, ArangoDriver defaultDb, ArangoDriver releasedDb) {
        Map<String, String> arangoNameMapping = getArangoNameMapping(defaultDb.getOrCreateDB());
        for (String vertexId : vertexIds) {
            String collectionName = namingConvention.getCollectionNameFromId(vertexId);
            ArangoCollection collection = defaultDb.getOrCreateDB().collection(collectionName);
            String documentKey = namingConvention.getKeyFromId(vertexId);
            if (collection.exists() && collection.documentExists(documentKey)) {
                String document = collection.getDocument(documentKey, String.class);
                ArangoCollection releaseCollection = releasedDb.getOrCreateDB().collection(collectionName);
                if (releaseCollection.exists() && releaseCollection.documentExists(documentKey)) {
                    replaceDocument(collectionName, documentKey, document, releasedDb);
                } else {
                    insertDocument(collectionName, arangoNameMapping.get(collectionName), document, collection.getInfo().getType(), releasedDb);
                }
                if (collection.getInfo().getType() == CollectionType.EDGES) {
                    Map released_doc = releaseCollection.getDocument(documentKey, Map.class);
                    if (released_doc.containsKey("_to")) {
                        createCollectionIfNotExists(namingConvention.getCollectionNameFromId(released_doc.get("_to").toString()), null, CollectionType.DOCUMENT, releasedDb);
                    }
                }
            }
        }
    }

    public Set<String> getEmbeddedInstances(List<String> ids, ArangoDriver arango, Set<String> edgeCollectionNames, Set<String> result) {
        for (String id : ids) {
            String keyFromReference = namingConvention.getIdFromReference(id, false);
            if (!result.contains(keyFromReference)) {
                result.add(keyFromReference);
                if (!edgeCollectionNames.isEmpty()) {
                    String arangoQuery = queryFactory.createEmbeddedInstancesQuery(edgeCollectionNames, keyFromReference, arango);
                    try {
                        ArangoCursor<Map> q = arango.getOrCreateDB().query(arangoQuery, null, new AqlQueryOptions(), Map.class);
                        List<Map> queryResult = q.asListRemaining();
                        if (queryResult != null) {
                            List<String> embeddedIds = queryResult.stream().filter(e -> (Boolean) e.get("isEmbedded")).map(e -> e.get("vertexId").toString()).collect(Collectors.toList());
                            if (!embeddedIds.isEmpty()) {
                                getEmbeddedInstances(embeddedIds, arango, edgeCollectionNames, result);
                                result.addAll(embeddedIds);
                            }
                            result.addAll(queryResult.stream().map(e -> e.get("edgeId").toString()).collect(Collectors.toSet()));
                        }
                    } catch (ArangoDBException e) {
                        logger.error("Arango query exception - {}", arangoQuery);
                        throw e;
                    }
                }
            }
        }
        return result;
    }

    @Override
    protected boolean alreadyExists(JsonLdVertex vertex, ArangoDriver transactionOrConnection) {
        String vertexLabel = namingConvention.getVertexLabel(vertex.getEntityName());
        ArangoCollection collection = transactionOrConnection.getOrCreateDB().collection(vertexLabel);
        return collection.exists() && collection.documentExists(namingConvention.getKey(vertex));
    }

    public Map<String, String> getArangoNameMapping(ArangoDatabase db) {
        String query = queryFactory.queryArangoNameMappings(NAME_LOOKUP_MAP);
        try {
            ArangoCursor<Map> q = db.query(query, null, new AqlQueryOptions(), Map.class);
            List<Map> instances = q.asListRemaining();
            return instances.stream().collect(HashMap::new, (map, item) -> map.put((String) item.get("arango"), (String) item.get("original")), Map::putAll);
        } catch (ArangoDBException e) {
            logger.error("Arango query exception - {}", query);
            throw e;
        }
    }

    public void deleteVertex(String entityName, String key, ArangoDriver arango) {
        deleteVertex(namingConvention.createId(entityName, key), arango);
    }

    public void deleteVertex(String id, ArangoDriver arango) {
        ArangoCollection collection = arango.getOrCreateDB().collection(namingConvention.getCollectionNameFromId(id));
        String keyFromId = namingConvention.getKeyFromId(id);
        if (keyFromId!=null && collection.exists() && collection.documentExists(keyFromId)) {
            Set<String> edgesCollectionNames = arango.getEdgesCollectionNames();
            Set<String> embeddedInstances = getEmbeddedInstances(Collections.singletonList(id), arango, edgesCollectionNames, new LinkedHashSet<>());
            for (String embeddedInstance : embeddedInstances) {
                deleteDocument(embeddedInstance, arango.getOrCreateDB());
            }
        } else {
            logger.warn("No deletion of {} because it does not exist in the database", id);
        }
    }

    void deleteDocument(String vertexId, ArangoDatabase db) {
        if (vertexId != null) {
            String documentId = namingConvention.getKeyFromId(vertexId);
            String collectionName = namingConvention.getCollectionNameFromId(vertexId);
            ArangoCollection collection = db.collection(collectionName);
            if (collection.exists() && collection.documentExists(documentId)) {
                logger.info("Delete document: {} from database {}", documentId, db.name());
                collection.deleteDocument(documentId);
            } else {
                logger.warn("Tried to delete {} although the collection doesn't exist. Skip.", vertexId);
            }
        } else {
            logger.error("Was not able to delete document due to missing id");
        }
    }


    public Map<String, Object> getPropertyCount(String collectionName, ArangoDatabase db) {
        String query = queryFactory.queryPropertyCount(collectionName);
        try {
            ArangoCursor<Map> result = db.query(query, null, new AqlQueryOptions(), Map.class);
            return result.asListRemaining().stream().sorted(Comparator.comparing(a -> ((String) a.get("attr")))).collect(LinkedHashMap::new, (map, item) -> map.put((String) item.get("attr"), (Long) item.get("count")), Map::putAll);
        } catch (ArangoDBException e) {
            logger.error("Arango query exception - {}", query);
            throw e;
        }
    }


    @Override
    protected void updateVertex(JsonLdVertex vertex, ArangoDriver arango) throws JSONException {
        replaceDocument(namingConvention.getVertexLabel(vertex.getEntityName()), namingConvention.getKey(vertex), toJSONString(vertex), arango);
    }

    public void replaceDocument(String collectionName, String documentKey, String jsonPayload, ArangoDriver arango) {
        if (collectionName != null && documentKey != null && jsonPayload != null) {
            logger.info("Update document: {}/{} in db {}", collectionName, documentKey, arango.getDatabaseName());
            logger.debug("Update document: {}/{} in db {} with payload {}", collectionName, documentKey, arango.getDatabaseName(), jsonPayload);
            arango.getOrCreateDB().collection(collectionName).replaceDocument(documentKey, jsonPayload);
        } else {
            logger.error("Incomplete data. Was not able to update the document in {}/{} with payload {}", collectionName, documentKey, jsonPayload);
        }
    }

    @Override
    protected void insertVertex(JsonLdVertex vertex, ArangoDriver arango) throws JSONException {
        insertVertexDocument(toJSONString(vertex), vertex.getEntityName(), arango);
    }

    public void insertVertexDocument(String jsonLd, String vertexName, ArangoDriver arango) {
        insertDocument(namingConvention.getVertexLabel(vertexName), vertexName, jsonLd, CollectionType.DOCUMENT, arango);
    }

    private void insertDocument(String collectionName, String originalName, String jsonLd, CollectionType collectionType, ArangoDriver arango) {
        if (collectionName != null && jsonLd != null) {
            ArangoCollection collection = createCollectionIfNotExists(collectionName, originalName, collectionType, arango);
            logger.info("Insert document: {} in db {}", collectionName, arango.getDatabaseName());
            logger.debug("Insert document: {} in db {} with payload {}", collectionName, arango.getDatabaseName(), jsonLd);
            collection.insertDocument(jsonLd);
        } else {
            logger.error("Incomplete data. Was not able to insert the document in {} with payload {} into database {}", collectionName, jsonLd, arango.getDatabaseName());
        }
    }

    private ArangoCollection createCollectionIfNotExists(String collectionName, String originalName, CollectionType collectionType, ArangoDriver arango) {
        ArangoDatabase db = arango.getOrCreateDB();
        ArangoCollection collection = db.collection(collectionName);
        if (!collection.exists()) {
            logger.info("Create {} collection {} in database {}", collectionType, collectionName, arango.getDatabaseName());
            CollectionCreateOptions collectionCreateOptions = new CollectionCreateOptions();
            collectionCreateOptions.type(collectionType);
            db.createCollection(collectionName, collectionCreateOptions);
            collection = db.collection(collectionName);
        }
        if (!collectionName.equals(NAME_LOOKUP_MAP) && originalName != null) {
            ArangoCollection namelookup = createCollectionIfNotExists(NAME_LOOKUP_MAP, null, CollectionType.DOCUMENT, arango);
            if (!namelookup.documentExists(collectionName)) {
                insertDocument(NAME_LOOKUP_MAP, null, String.format("{\"originalName\": \"%s\", \"_key\": \"%s\"}", originalName, collectionName), CollectionType.DOCUMENT, arango);
            }
        }
        return collection;
    }

    @Override
    protected void createEdge(JsonLdVertex vertex, JsonLdEdge edge, ArangoDriver arango) throws JSONException {
        insertDocument(namingConvention.getEdgeLabel(edge), null, createEdgeDocument(vertex, edge, arango).toString(), CollectionType.EDGES, arango);
    }

    @Override
    protected void replaceEdge(JsonLdVertex vertex, JsonLdEdge edge, ArangoDriver arango) throws JSONException {
        replaceDocument(namingConvention.getEdgeLabel(edge), namingConvention.getReferenceKey(vertex, edge), createEdgeDocument(vertex, edge, arango).toString(), arango);
    }

    @Override
    protected void removeEdge(JsonLdVertex vertex, JsonLdEdge edge, ArangoDriver transactionOrConnection) throws JSONException {
        deleteDocument(namingConvention.getEdgeTarget(edge), transactionOrConnection.getOrCreateDB());
    }

    private JSONObject createEdgeDocument(JsonLdVertex vertex, JsonLdEdge edge, ArangoDriver arango) throws JSONException {
        JSONObject o = new JSONObject();
        String from = namingConvention.getId(vertex);
        o.put("_from", from);
        String to = namingConvention.getEdgeTarget(edge);
        o.put("_to", to);
        createCollectionIfNotExists(namingConvention.getCollectionNameFromId(to), null, CollectionType.DOCUMENT, arango);
        String key = namingConvention.getReferenceKey(vertex, edge);
        o.put("_key", key);
        if (edge.getOrderNumber() != null && edge.getOrderNumber() >= 0) {
            o.put("orderNumber", edge.getOrderNumber());
        }
        for (JsonLdProperty jsonLdProperty : edge.getProperties()) {
            o.put(jsonLdProperty.getName(), jsonLdProperty.getValue());
        }
        o.put(JsonLdConsts.ID, null);
        return o;
    }


    @Override
    protected boolean hasEdge(JsonLdVertex vertex, JsonLdEdge edge, ArangoDriver arango) throws JSONException {
        ArangoDatabase db = arango.getOrCreateDB();
        String edgeLabel = namingConvention.getEdgeLabel(edge);
        String from = namingConvention.getId(vertex);
        String to = namingConvention.getEdgeTarget(edge);
        try {
            String query = queryFactory.queryEdgeByFromAndTo(edgeLabel, from, to);
            ArangoCursor<String> q = db.query(query, null, new AqlQueryOptions(), String.class);
            return q.hasNext();
        } catch (ArangoDBException e) {
            return false;
        }
    }


    @Override
    public void updateUnresolved(JsonLdVertex vertex, ArangoDriver arango) {

    }


    private JSONObject recreateObjectFromProperties(Set<JsonLdProperty> properties) throws JSONException {
        JSONObject o = new JSONObject();
        for (JsonLdProperty jsonLdProperty : properties) {
            if (jsonLdProperty.getName() != null) {
                if(jsonLdProperty.getValue() instanceof  JsonLdProperty){
                    JsonLdProperty nestedProperty = (JsonLdProperty)jsonLdProperty.getValue();
                    JSONObject o2 = new JSONObject();
                    o2.put(nestedProperty.getName(), nestedProperty.getValue());
                    o.put(jsonLdProperty.getName(), o2);
                }
                else if(jsonLdProperty.getValue() instanceof Collection){
                    JSONArray array = new JSONArray();
                    for (Object child : ((Collection) jsonLdProperty.getValue())) {
                        if(child instanceof  JsonLdProperty){
                            JsonLdProperty nestedProperty = (JsonLdProperty)jsonLdProperty.getValue();
                            JSONObject o2 = new JSONObject();
                            o2.put(nestedProperty.getName(), nestedProperty.getValue());
                            array.put(nestedProperty);
                        }
                        else{
                            array.put(child);
                        }
                    }
                    o.put(jsonLdProperty.getName(), array);
                }
                else {
                    o.put(jsonLdProperty.getName(), jsonLdProperty.getValue());
                }
            }
        }
        return o;
    }

    private String toJSONString(JsonLdVertex vertex) throws JSONException {
        //rebuildEmbeddedDocumentFromEdges(vertex);
        JSONObject o = recreateObjectFromProperties(vertex.getProperties());
        o.put("_key", namingConvention.getKey(vertex));
        if(!vertex.isEmbedded()) {
            o.put(JsonLdConsts.ID, String.format("%s/%s", vertex.getEntityName(), namingConvention.getKey(vertex)));
        }
        return o.toString(4);
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

    public String getTargetVertexId(JsonLdEdge edge, ArangoDriver arango) {
        return arango.getOrCreateDB().getDocument(namingConvention.getEdgeTarget(edge), Map.class).get("_to").toString();
    }


    @Override
    public List<JsonLdEdge> getEdgesToBeRemoved(JsonLdVertex vertex, ArangoDriver arango) {
        Set<String> idsOfNewEdges = vertex.getEdges().stream().map(e -> namingConvention.createId(namingConvention.getEdgeLabel(e.getName()), namingConvention.getReferenceKey(vertex, e))).collect(Collectors.toSet());
        //Query for edges of this vertex not containing the edges to be created
        Set<String> edgesCollectionNames = arango.getEdgesCollectionNames();
        if (!edgesCollectionNames.isEmpty()) {
            String query = queryFactory.queryEdgesToBeRemoved(namingConvention.getId(vertex), edgesCollectionNames, idsOfNewEdges, arango);
            try {
                List<String> ids = arango.getOrCreateDB().query(query, null, new AqlQueryOptions(), String.class).asListRemaining();
                return ids.parallelStream().map(i -> {
                    JsonLdEdge e = new JsonLdEdge();
                    e.setReference(i);
                    return e;
                }).collect(Collectors.toList());
            } catch (ArangoDBException e) {
                logger.error("Arango query exception - {}", query);
                throw e;
            }
        }
        return Collections.emptyList();
    }


    public void clearDatabase(ArangoDatabase db) {
        for (CollectionEntity collectionEntity : db.getCollections()) {
            if (!collectionEntity.getName().startsWith("_")) {
                logger.info("Drop collection {} in db {}", collectionEntity.getName(), db.name());
                db.collection(collectionEntity.getName()).drop();
            }
        }
    }

    public <T> List<T> getAll(String collectionName, Class<T> clazz, ArangoDriver driver) {
        String query = queryFactory.getAll(collectionName);
        try {
            return driver.getOrCreateDB().query(query, null, new AqlQueryOptions(), clazz).asListRemaining();
        } catch (ArangoDBException e) {
            logger.error("Arango query exception - {}", query);
            throw e;
        }
    }

    public List<Map> inDepthGraph(String vertex, Integer step, ArangoDriver driver) {
        ArangoDatabase db = driver.getOrCreateDB();
        Set<String> edgesCollections = driver.getEdgesCollectionNames();
        String query = queryFactory.queryInDepthGraph(edgesCollections, vertex, step, driver);
        try {
            ArangoCursor<Map> q = db.query(query, null, new AqlQueryOptions(), Map.class);
            return q.asListRemaining();
        } catch (ArangoDBException e) {
            logger.error("Arango query exception - {}", query);
            throw e;
        }
    }

}
