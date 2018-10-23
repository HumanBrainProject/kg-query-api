package org.humanbrainproject.knowledgegraph.propertyGraph.arango.control;

import com.arangodb.ArangoCollection;
import com.arangodb.ArangoCursor;
import com.arangodb.ArangoDBException;
import com.arangodb.ArangoDatabase;
import com.arangodb.entity.CollectionEntity;
import com.arangodb.entity.CollectionType;
import com.arangodb.model.AqlQueryOptions;
import com.arangodb.model.CollectionCreateOptions;
import com.github.jsonldjava.core.JsonLdConsts;
import deprecated.entity.Tuple;
import org.humanbrainproject.knowledgegraph.indexing.control.inference.InferenceController;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusInstanceReference;
import org.humanbrainproject.knowledgegraph.jsonld.control.JsonLdToVerticesAndEdges;
import org.humanbrainproject.knowledgegraph.jsonld.control.JsonTransformer;
import org.humanbrainproject.knowledgegraph.propertyGraph.arango.control.query.ArangoQueryFactory;
import org.humanbrainproject.knowledgegraph.propertyGraph.arango.entity.ArangoCollectionReference;
import org.humanbrainproject.knowledgegraph.propertyGraph.arango.entity.ArangoDocumentReference;
import org.humanbrainproject.knowledgegraph.propertyGraph.control.VertexRepository;
import org.humanbrainproject.knowledgegraph.propertyGraph.entity.MainVertex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.*;

@Primary
@Component
public class ArangoRepository extends VertexRepository<ArangoConnection, ArangoDocumentReference> {

    @Autowired
    ArangoQueryFactory queryFactory;

    @Autowired
    JsonTransformer transformer;

    @Autowired
    JsonLdToVerticesAndEdges jsonLdToVerticesAndEdges;

    @Autowired
    ArangoDatabaseFactory databaseFactory;

    @Override
    public void clearDatabase(ArangoConnection connection) {

    }

    public NexusInstanceReference findOriginalId(NexusInstanceReference reference){
        ArangoDocumentReference arangoDocumentReference = ArangoDocumentReference.fromNexusInstance(reference);
        Map byKey = getByKey(arangoDocumentReference, Map.class, databaseFactory.getDefaultDB());
        Object originalParent = byKey.get(InferenceController.ORIGINAL_PARENT_PROPERTY);
        if(originalParent instanceof Map){
            String id = (String)((Map) originalParent).get(JsonLdConsts.ID);
            return NexusInstanceReference.createFromUrl(id);
        }
        else{
            return reference;
        }
    }

    public String getPayloadById(ArangoDocumentReference documentReference, ArangoConnection arangoConnection){
        return arangoConnection.getOrCreateDB().getDocument(documentReference.getId(), String.class);
    }

    @Override
    public MainVertex getVertexStructureById(ArangoDocumentReference documentReference, ArangoConnection arango){
        //TODO implement
        return null;
    }


    private static final ArangoCollectionReference NAME_LOOKUP_MAP = new ArangoCollectionReference("name_lookup");

    public <T> T getByKey(ArangoDocumentReference document, Class<T> clazz, ArangoConnection arango) {
        return arango.getOrCreateDB().collection(document.getCollection().getName()).getDocument(document.getKey(), clazz);
    }

    public Tuple<String, Long> countInstances(ArangoCollectionReference collection, ArangoConnection arango) {
        Long count = arango.getOrCreateDB().collection(collection.getName()).count().getCount();
        return new Tuple<>(collection.getName(), count);
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

    void deleteDocument(ArangoDocumentReference document, ArangoDatabase db) {
        if (document != null) {
            ArangoCollection collection = db.collection(document.getCollection().getName());
            if (collection.exists() && collection.documentExists(document.getId())) {
                logger.info("Delete document: {} from database {}", document.getId(), db.name());
                collection.deleteDocument(document.getId());
            } else {
                logger.warn("Tried to delete {} although the collection doesn't exist. Skip.", document.getId());
            }
        } else {
            logger.error("Was not able to delete document due to missing id");
        }
    }


    public Map<String, Object> getPropertyCount(ArangoCollectionReference collection, ArangoDatabase db) {
        String query = queryFactory.queryPropertyCount(collection);
        try {
            ArangoCursor<Map> result = db.query(query, null, new AqlQueryOptions(), Map.class);
            return result.asListRemaining().stream().sorted(Comparator.comparing(a -> ((String) a.get("attr")))).collect(LinkedHashMap::new, (map, item) -> map.put((String) item.get("attr"), (Long) item.get("count")), Map::putAll);
        } catch (ArangoDBException e) {
            logger.error("Arango query exception - {}", query);
            throw e;
        }
    }

    public void replaceDocument(ArangoDocumentReference document, String jsonPayload, ArangoConnection arango) {
        if (jsonPayload != null) {
            logger.info("Update document: {} in db {}", document.getId(), arango.getDatabaseName());
            logger.debug("Update document: {} in db {} with payload {}", document.getId(), arango.getDatabaseName(), jsonPayload);
            arango.getOrCreateDB().collection(document.getCollection().getName()).replaceDocument(document.getKey(), jsonPayload);
        } else {
            logger.warn("Incomplete data. Was not able to update the document in {} because of a null-payload", document.getId());
        }
    }

    private void insertDocument(ArangoCollectionReference collectionReference, String originalName, String jsonLd, CollectionType collectionType, ArangoConnection arango) {
        if (jsonLd != null) {
            ArangoCollection collection = createCollectionIfNotExists(collectionReference, originalName, collectionType, arango);
            logger.info("Insert document: {} in db {}", collectionReference.getName(), arango.getDatabaseName());
            logger.debug("Insert document: {} in db {} with payload {}", collectionReference.getName(), arango.getDatabaseName(), jsonLd);
            collection.insertDocument(jsonLd);
        } else {
            logger.warn("Incomplete data. Was not able to insert the document in {} because of a null-payload into database {}", collectionReference.getName(), arango.getDatabaseName());
        }
    }

    private ArangoCollection createCollectionIfNotExists(ArangoCollectionReference collectionReference, String originalName, CollectionType collectionType, ArangoConnection arango) {
        ArangoDatabase db = arango.getOrCreateDB();
        ArangoCollection collection = db.collection(collectionReference.getName());
        if (!collection.exists()) {
            logger.info("Create {} collection {} in database {}", collectionType, collectionReference.getName(), arango.getDatabaseName());
            CollectionCreateOptions collectionCreateOptions = new CollectionCreateOptions();
            collectionCreateOptions.type(collectionType);
            db.createCollection(collectionReference.getName(), collectionCreateOptions);
            collection = db.collection(collectionReference.getName());
        }
        if (!collectionReference.getName().equals(NAME_LOOKUP_MAP) && originalName != null) {
            ArangoCollection namelookup = createCollectionIfNotExists(NAME_LOOKUP_MAP, null, CollectionType.DOCUMENT, arango);
            if (!namelookup.documentExists(collectionReference.getName())) {
                insertDocument(NAME_LOOKUP_MAP, null, String.format("{\"originalName\": \"%s\", \"_key\": \"%s\"}", originalName, collectionReference.getName()), CollectionType.DOCUMENT, arango);
            }
        }
        return collection;
    }

    public void clearDatabase(ArangoDatabase db) {
        for (CollectionEntity collectionEntity : db.getCollections()) {
            if (!collectionEntity.getName().startsWith("_")) {
                logger.info("Drop collection {} in db {}", collectionEntity.getName(), db.name());
                db.collection(collectionEntity.getName()).drop();
            }
        }
    }

    public <T> List<T> getAll(ArangoCollectionReference collection, Class<T> clazz, ArangoConnection driver) {
        String query = queryFactory.getAll(collection);
        try {
            return driver.getOrCreateDB().query(query, null, new AqlQueryOptions(), clazz).asListRemaining();
        } catch (ArangoDBException e) {
            logger.error("Arango query exception - {}", query);
            throw e;
        }
    }

    public List<Map> inDepthGraph(ArangoDocumentReference document, Integer step, ArangoConnection driver) {
        ArangoDatabase db = driver.getOrCreateDB();
        Set<ArangoCollectionReference> edgesCollections = driver.getEdgesCollectionNames();
        String query = queryFactory.queryInDepthGraph(edgesCollections, document, step, driver);
        try {
            ArangoCursor<Map> q = db.query(query, null, new AqlQueryOptions(), Map.class);
            return q.asListRemaining();
        } catch (ArangoDBException e) {
            logger.error("Arango query exception - {}", query);
            throw e;
        }
    }


    public List<Map> releaseGraph(ArangoDocumentReference document, Integer maxDepth, ArangoConnection driver) {
        ArangoDatabase db = driver.getOrCreateDB();
        String query = queryFactory.queryReleaseGraph(driver.getEdgesCollectionNames(), document, maxDepth, driver);
        ArangoCursor<Map> q = db.query(query, null, new AqlQueryOptions(), Map.class);
        return q.asListRemaining();
    }

    public List<Map> getDocument(ArangoDocumentReference document, ArangoConnection driver) {
        ArangoDatabase db = driver.getOrCreateDB();
        String query = queryFactory.getDocument(document);
        ArangoCursor<Map> q = db.query(query, null, new AqlQueryOptions(), Map.class);
        return q.asListRemaining();
    }

    public List<Map> getGetEditorSpecDocument(ArangoCollectionReference collection, ArangoConnection driver) {
        ArangoDatabase db = driver.getOrCreateDB();
        String query = queryFactory.getGetEditorSpecDocument(collection);
        ArangoCursor<Map> q = db.query(query, null, new AqlQueryOptions(), Map.class);
        return q.asListRemaining();
    }

    public Map getInstanceList(ArangoCollectionReference collection, Integer from, Integer size, String searchTerm, ArangoConnection driver) {
        ArangoDatabase db = driver.getOrCreateDB();
        //TODO check what does that function do? Still required with the reconciled switch?
//        String c = String.join("reconciled-", collection.split("-", 2));
//        String recCollection = driver.getCollectionLabels().stream().noneMatch(el -> el.equals(c)) ? "[]" : "`" + c + "`";
//        String query = queryFactory.getInstanceList(collection, from, size, searchTerm, recCollection);
        AqlQueryOptions options = new AqlQueryOptions().count(true).fullCount(true);
        Map m = new HashMap();
//        try {
////            ArangoCursor<Map> q = db.query(query, null, options, Map.class);
//            m.put("count", q.getCount());
//            m.put("fullCount", q.getStats().getFullCount());
//            m.put("data", q.asListRemaining());
//        } catch (ArangoDBException e) {
//            if (e.getResponseCode() == 404) {
//                m.put("count", 0);
//                m.put("fullCount", 0);
//                m.put("data", new ArrayList<Map>());
//            } else {
//                throw e;
//            }
//        }
        return m;
    }

    public List<Map> getReleaseStatus(ArangoDocumentReference document, ArangoConnection driver) {
        ArangoDatabase db = driver.getOrCreateDB();
        Set<ArangoCollectionReference> edgesCollections = driver.getEdgesCollectionNames();
        String query = queryFactory.releaseStatus(edgesCollections, document, driver);
        ArangoCursor<Map> q = db.query(query, null, new AqlQueryOptions(), Map.class);
        return q.asListRemaining();
    }


}
