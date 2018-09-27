package org.humanbrainproject.knowledgegraph.control.arango;

import com.arangodb.ArangoCollection;
import com.arangodb.ArangoCursor;
import com.arangodb.ArangoDBException;
import com.arangodb.ArangoDatabase;
import com.arangodb.entity.AqlFunctionEntity;
import com.arangodb.entity.CollectionEntity;
import com.arangodb.entity.CollectionType;
import com.arangodb.model.AqlFunctionCreateOptions;
import com.arangodb.model.AqlFunctionGetOptions;
import com.arangodb.model.AqlQueryOptions;
import com.arangodb.model.CollectionCreateOptions;
import org.humanbrainproject.knowledgegraph.control.Configuration;
import org.humanbrainproject.knowledgegraph.control.VertexRepository;
import org.humanbrainproject.knowledgegraph.control.json.JsonTransformer;
import org.humanbrainproject.knowledgegraph.control.jsonld.JsonLdToVerticesAndEdges;
import org.humanbrainproject.knowledgegraph.entity.Tuple;
import org.humanbrainproject.knowledgegraph.entity.indexing.GraphIndexingSpec;
import org.humanbrainproject.knowledgegraph.entity.jsonld.JsonLdEdge;
import org.humanbrainproject.knowledgegraph.entity.jsonld.JsonLdVertex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class ArangoRepository extends VertexRepository<ArangoDriver> {

    @Autowired
    ArangoNamingConvention namingConvention;

    @Autowired
    Configuration configuration;

    @Autowired
    ArangoQueryFactory queryFactory;

    @Autowired
    JsonTransformer transformer;

    @Autowired
    JsonLdToVerticesAndEdges jsonLdToVerticesAndEdges;


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
            if(keyFromReference==null){
                if(!result.contains(id)) {
                    result.add(id);
                }
            }
            else if (!result.contains(keyFromReference)) {
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
    protected void updateVertex(JsonLdVertex vertex, ArangoDriver arango) {
        replaceDocument(namingConvention.getVertexLabel(vertex.getEntityName()), namingConvention.getKey(vertex), transformer.vertexToJSONString(vertex), arango);
    }

    public void replaceDocument(String collectionName, String documentKey, String jsonPayload, ArangoDriver arango) {
        if (collectionName != null && documentKey != null && jsonPayload != null) {
            logger.info("Update document: {}/{} in db {}", collectionName, documentKey, arango.getDatabaseName());
            logger.debug("Update document: {}/{} in db {} with payload {}", collectionName, documentKey, arango.getDatabaseName(), jsonPayload);
            arango.getOrCreateDB().collection(collectionName).replaceDocument(documentKey, jsonPayload);
        } else {
            logger.warn("Incomplete data. Was not able to update the document in {}/{} with payload {}", collectionName, documentKey, jsonPayload);
        }
    }

    @Override
    protected void insertVertex(JsonLdVertex vertex, ArangoDriver arango) {
        insertVertexDocument(transformer.vertexToJSONString(vertex), vertex.getEntityName(), arango);
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
            logger.warn("Incomplete data. Was not able to insert the document in {} with payload {} into database {}", collectionName, jsonLd, arango.getDatabaseName());
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


    private void ensureTargetCollectionForEdge(JsonLdEdge edge, ArangoDriver arango){
        String target = namingConvention.getEdgeTarget(edge);
        if(target!=null){
            createCollectionIfNotExists(namingConvention.getCollectionNameFromId(target), null, CollectionType.DOCUMENT, arango);
        }
    }

    @Override
    protected void createEdge(JsonLdVertex vertex, JsonLdEdge edge, ArangoDriver arango) {
        ensureTargetCollectionForEdge(edge, arango);
        String edgeDocument = transformer.edgeToJSONString(vertex, edge);
        if(edgeDocument!=null) {
            insertDocument(namingConvention.getEdgeLabel(edge), null, edgeDocument, CollectionType.EDGES, arango);
        }
    }

    @Override
    protected void replaceEdge(JsonLdVertex vertex, JsonLdEdge edge, ArangoDriver arango) {
        ensureTargetCollectionForEdge(edge, arango);
        String edgeDocument = transformer.edgeToJSONString(vertex, edge);
        if(edgeDocument!=null) {
            replaceDocument(namingConvention.getEdgeLabel(edge), namingConvention.getReferenceKey(vertex, edge), edgeDocument, arango);
        }
    }



    @Override
    protected void removeEdge(JsonLdVertex vertex, JsonLdEdge edge, ArangoDriver transactionOrConnection) {
        List<JsonLdVertex> targetVertices = Collections.emptyList();
        if(edge.getTarget()!=null){
            targetVertices = Collections.singletonList(edge.getTarget());
        }
        else if(edge.getReference()!=null){
            String edgeName = namingConvention.getCollectionNameFromId(edge.getReference());
            String edgeKey = namingConvention.getKeyFromId(edge.getReference());
            Map edgeContent = getByKey(edgeName, edgeKey, Map.class, transactionOrConnection);
            if(edgeContent.containsKey("_to") && edgeContent.get("_to")!=null){
                String reference = edgeContent.get("_to").toString();
                String targetName = namingConvention.getCollectionNameFromId(reference);
                String targetKey = namingConvention.getKeyFromId(reference);
                Map target = getByKey(targetName, targetKey, Map.class, transactionOrConnection);
                GraphIndexingSpec spec = new GraphIndexingSpec().setEntityName(targetName).setId(targetKey);
                targetVertices = jsonLdToVerticesAndEdges.transformFullyQualifiedJsonLdToVerticesAndEdges(spec, target);
            }
        }
        for (JsonLdVertex targetVertex : targetVertices) {
            if(targetVertex.isEmbedded()){
                deleteVertex(targetVertex.getEntityName(), targetVertex.getKey(), transactionOrConnection);
            }
        }
        deleteDocument(namingConvention.getEdgeTarget(edge), transactionOrConnection.getOrCreateDB());
    }

    @Override
    protected boolean hasEdge(JsonLdVertex vertex, JsonLdEdge edge, ArangoDriver arango) {
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


    public List<Map> releaseGraph(String vertex, Integer maxDepth, ArangoDriver driver){
        ArangoDatabase db = driver.getOrCreateDB();
        Set<String> edgesCollections = driver.getEdgesCollectionNames();
        String query = queryFactory.queryReleaseGraph(edgesCollections, vertex, maxDepth, driver);
        ArangoCursor<Map> q = db.query(query, null, new AqlQueryOptions(), Map.class );
        return q.asListRemaining();
    }

    public List<Map> getDocument(String documentID,  ArangoDriver driver){
        ArangoDatabase db = driver.getOrCreateDB();
        String query = queryFactory.getDocument(documentID);
        ArangoCursor<Map> q = db.query(query, null, new AqlQueryOptions(), Map.class );
        return q.asListRemaining();
    }
    public Map getInstanceList(String collection, Integer from, Integer size, String searchTerm, ArangoDriver driver){
        ArangoDatabase db = driver.getOrCreateDB();
        String c = String.join("reconciled-",collection.split("-", 2));
        String recCollection = driver.getCollectionLabels().stream().noneMatch( el ->  el.equals(c)) ? "[]" : "`"+c+"`";
        String query = queryFactory.getInstanceList(collection, from , size, searchTerm, recCollection);
        AqlQueryOptions options = new AqlQueryOptions().count(true).fullCount(true);
        Map m = new HashMap();
        try{
            ArangoCursor<Map> q = db.query(query, null, options, Map.class );
            m.put("count", q.getCount());
            m.put("fullCount", q.getStats().getFullCount());
            m.put("data", q.asListRemaining());
        }catch (ArangoDBException e){
            if(e.getResponseCode() == 404){
                m.put("count", 0);
                m.put("fullCount", 0);
                m.put("data", new ArrayList<Map>());
            }else{
                throw e;
            }
        }
        return m;
    }

    public List<Map> getReleaseStatus(String documentID, String reconciledId, ArangoDriver driver){
        ArangoDatabase db = driver.getOrCreateDB();
        Set<String> edgesCollections = driver.getEdgesCollectionNames();
        String query = queryFactory.releaseStatus(edgesCollections, documentID, reconciledId, driver);
        ArangoCursor<Map> q = db.query(query, null, new AqlQueryOptions(), Map.class );
        return q.asListRemaining();
    }
}
