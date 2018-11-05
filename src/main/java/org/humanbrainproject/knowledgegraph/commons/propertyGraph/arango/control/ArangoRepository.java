package org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control;

import com.arangodb.ArangoCollection;
import com.arangodb.ArangoCursor;
import com.arangodb.ArangoDBException;
import com.arangodb.ArangoDatabase;
import com.arangodb.entity.CollectionEntity;
import com.arangodb.entity.CollectionType;
import com.arangodb.model.AqlQueryOptions;
import com.arangodb.model.CollectionCreateOptions;
import com.github.jsonldjava.core.JsonLdConsts;
import org.humanbrainproject.knowledgegraph.commons.jsonld.control.JsonTransformer;
import org.humanbrainproject.knowledgegraph.commons.nexus.control.NexusConfiguration;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.query.ArangoQueryFactory;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoCollectionReference;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoDocumentReference;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.exceptions.UnexpectedNumberOfResults;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.exceptions.UnexpectedResultStructure;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.control.VertexRepository;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.entity.Tuple;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.entity.Vertex;
import org.humanbrainproject.knowledgegraph.commons.vocabulary.HBPVocabulary;
import org.humanbrainproject.knowledgegraph.indexing.control.MessageProcessor;
import org.humanbrainproject.knowledgegraph.indexing.entity.IndexingMessage;
import org.humanbrainproject.knowledgegraph.indexing.entity.QualifiedIndexingMessage;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusInstanceReference;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusSchemaReference;
import org.humanbrainproject.knowledgegraph.releasing.entity.ReleaseStatus;
import org.humanbrainproject.knowledgegraph.releasing.entity.ReleaseStatusResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Primary
@Component
public class ArangoRepository extends VertexRepository<ArangoConnection, ArangoDocumentReference> {

    @Autowired
    ArangoQueryFactory queryFactory;

    @Autowired
    JsonTransformer transformer;

    @Autowired
    ArangoDatabaseFactory databaseFactory;

    @Autowired
    NexusConfiguration configuration;

    @Autowired
    MessageProcessor messageProcessor;

    @Override
    public void clearDatabase(ArangoConnection connection) {

    }

    public NexusInstanceReference findOriginalId(NexusInstanceReference reference) {
        ArangoDocumentReference arangoDocumentReference = ArangoDocumentReference.fromNexusInstance(reference);
        Map byKey = getByKey(arangoDocumentReference, Map.class, databaseFactory.getDefaultDB());
        if (byKey != null) {
            Object originalParent = byKey.get(HBPVocabulary.INFERENCE_EXTENDS);
            if (originalParent == null) {
                originalParent = byKey.get(HBPVocabulary.INFERENCE_OF);
            }
            if (originalParent instanceof Map) {
                String id = (String) ((Map) originalParent).get(JsonLdConsts.ID);
                return NexusInstanceReference.createFromUrl(id);
            }
        }
        return reference;
    }

    public Set<String> findOriginalIdsWithLinkTo(ArangoDocumentReference instanceReference, ArangoCollectionReference collectionReference, ArangoConnection arangoConnection) {
        Set<ArangoCollectionReference> collections = arangoConnection.getCollections();
        if (collections.contains(instanceReference.getCollection()) && collections.contains(collectionReference)) {
            String query = queryFactory.queryOriginalIdForLink(instanceReference, collectionReference);
            return arangoConnection.getOrCreateDB().query(query, null, new AqlQueryOptions(), String.class).asListRemaining().stream().filter(Objects::nonNull).collect(Collectors.toSet());
        }
        return Collections.emptySet();
    }


    public String getPayloadById(ArangoDocumentReference documentReference, ArangoConnection arangoConnection) {
        return arangoConnection.getOrCreateDB().getDocument(documentReference.getId(), String.class);
    }

    public Map getDocument(ArangoDocumentReference documentReference, ArangoConnection arangoConnection) {
        return arangoConnection.getOrCreateDB().getDocument(documentReference.getId(), Map.class);
    }


    public Set<ArangoDocumentReference> getReferencesBelongingToInstance(NexusInstanceReference nexusInstanceReference, ArangoConnection arangoConnection) {
        Set<ArangoCollectionReference> collections = new HashSet<>(arangoConnection.getEdgesCollectionNames());
        String query = queryFactory.queryForIdsWithProperty("_originalId", nexusInstanceReference.getFullId(true), collections);
        List<List> result = query == null ? new ArrayList<>() : arangoConnection.getOrCreateDB().query(query, null, new AqlQueryOptions(), List.class).asListRemaining();
        if (result.size() == 1) {
            return ((List<String>) result.get(0)).stream().filter(Objects::nonNull).map(id -> ArangoDocumentReference.fromId(id.toString())).collect(Collectors.toSet());
        }
        return new LinkedHashSet<>();
    }


    @Override
    public Vertex getVertexStructureById(ArangoDocumentReference documentReference, ArangoConnection arango) {
        Map document = arango.getOrCreateDB().getDocument(documentReference.getId(), Map.class);
        if (document != null) {
            NexusInstanceReference reference = NexusInstanceReference.createFromUrl(document.get("_originalId").toString());
            QualifiedIndexingMessage qualified = messageProcessor.qualify(new IndexingMessage(reference, transformer.getMapAsJson(document), null, null));
            return messageProcessor.createVertexStructure(qualified);
        }
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


    public void insertDocument(ArangoDocumentReference document, String documentPayload, CollectionType
            collectionType, ArangoDatabase db) {
        if (document != null && documentPayload != null) {
            ArangoCollection collection = db.collection(document.getCollection().getName());
            if (!collection.exists()) {
                db.createCollection(document.getCollection().getName(), new CollectionCreateOptions().type(collectionType));
                logger.info("Created collection {} in database {}", document.getCollection().getName(), db.name());
                collection = db.collection(document.getCollection().getName());
            }
            if (collection.documentExists(document.getKey())) {
                updateDocument(document, documentPayload, collectionType, db);
            } else {
                try {
                    collection.insertDocument(documentPayload);
                    logger.info("Inserted document: {} in database {}", document.getId(), db.name());
                } catch (ArangoDBException dbexception) {
                    logger.error(String.format("Was not able to insert document: %s in database %s", document.getId(), db.name()), dbexception);
                    throw dbexception;
                }
            }
        }
    }

    private void updateDocument(ArangoDocumentReference document, String documentPayload, CollectionType
            collectionType, ArangoDatabase db) {
        if (document != null && documentPayload != null) {
            ArangoCollection collection = db.collection(document.getCollection().getName());
            if (!collection.exists() || !collection.documentExists(document.getKey())) {
                insertDocument(document, documentPayload, collectionType, db);
            } else {
                try {
                    collection.updateDocument(document.getKey(), documentPayload);
                    logger.info("Updated document: {} in database {}", document.getId(), db.name());
                } catch (ArangoDBException dbexception) {
                    logger.error(String.format("Was not able to update document: %s in database %s", document.getId(), db.name()), dbexception);
                    throw dbexception;
                }
            }
        }

    }

    public void deleteOutgoingRelations(ArangoDocumentReference document, ArangoConnection connection) {
        if (document != null) {
            ArangoDatabase db = connection.getOrCreateDB();
            ArangoCollection collection = db.collection(document.getCollection().getName());
            if (collection.exists()) {
                if (collection.documentExists(document.getKey())) {
                    try {
                        ArangoCursor<String> result = db.query(queryFactory.queryOutboundRelationsForDocument(document, connection.getEdgesCollectionNames()), null, new AqlQueryOptions(), String.class);
                        for (String id : result.asListRemaining()) {
                            deleteDocument(ArangoDocumentReference.fromId(id), db);
                        }
                        logger.info("Deleted document: {} from database {}", document.getId(), db.name());
                    } catch (ArangoDBException dbexception) {
                        logger.error(String.format("Was not able to delete document: %s in database %s", document.getId(), db.name()), dbexception);
                        throw dbexception;
                    }
                } else {
                    logger.warn("Was not able to delete {} because the document does not exist. Skip.", document.getId());
                }
            } else {
                logger.warn("Tried to delete {} although the collection doesn't exist. Skip.", document.getId());
            }
        } else {
            logger.error("Was not able to delete document due to missing id");
        }
    }


    public void deleteDocument(ArangoDocumentReference document, ArangoDatabase db) {
        if (document != null) {
            ArangoCollection collection = db.collection(document.getCollection().getName());
            if (collection.exists()) {
                if (collection.documentExists(document.getKey())) {
                    try {
                        collection.deleteDocument(document.getKey());
                        logger.info("Deleted document: {} from database {}", document.getId(), db.name());
                    } catch (ArangoDBException dbexception) {
                        logger.error(String.format("Was not able to delete document: %s in database %s", document.getId(), db.name()), dbexception);
                        throw dbexception;
                    }
                } else {
                    logger.warn("Was not able to delete {} because the document does not exist. Skip.", document.getId());
                }
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

    private void insertDocument(ArangoCollectionReference collectionReference, String originalName, String
            jsonLd, CollectionType collectionType, ArangoConnection arango) {
        if (jsonLd != null) {
            ArangoCollection collection = createCollectionIfNotExists(collectionReference, originalName, collectionType, arango);
            logger.info("Insert document: {} in db {}", collectionReference.getName(), arango.getDatabaseName());
            logger.debug("Insert document: {} in db {} with payload {}", collectionReference.getName(), arango.getDatabaseName(), jsonLd);
            collection.insertDocument(jsonLd);
        } else {
            logger.warn("Incomplete data. Was not able to insert the document in {} because of a null-payload into database {}", collectionReference.getName(), arango.getDatabaseName());
        }
    }

    private ArangoCollection createCollectionIfNotExists(ArangoCollectionReference collectionReference, String
            originalName, CollectionType collectionType, ArangoConnection arango) {
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

    public List<Map> inDepthGraph(ArangoDocumentReference document, Integer step, ArangoConnection connection) {
        ArangoDatabase db = connection.getOrCreateDB();
        Set<ArangoCollectionReference> edgesCollections = connection.getEdgesCollectionNames();
        String query = queryFactory.queryInDepthGraph(edgesCollections, document, step, connection);
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

    public List<Map> getDocumentWithReleaseStatus(ArangoDocumentReference document, ArangoConnection driver) {
        ArangoDatabase db = driver.getOrCreateDB();
        String query = queryFactory.getDocumentWithReleaseStatus(document);
        ArangoCursor<Map> q = db.query(query, null, new AqlQueryOptions(), Map.class);
        return q.asListRemaining();
    }

    public List<Map> getGetEditorSpecDocument(ArangoCollectionReference collection, ArangoConnection driver) {
        ArangoDatabase db = driver.getOrCreateDB();
        String query = queryFactory.getGetEditorSpecDocument(collection);
        ArangoCursor<Map> q = db.query(query, null, new AqlQueryOptions(), Map.class);
        return q.asListRemaining();
    }

    public Map getInstanceList(ArangoCollectionReference collection, Integer from, Integer size, String
            searchTerm, ArangoConnection driver) {
        ArangoDatabase db = driver.getOrCreateDB();
        String query = queryFactory.getInstanceList(collection, from, size, searchTerm);
        AqlQueryOptions options = new AqlQueryOptions().count(true).fullCount(true);
        Map m = new HashMap();
        try {
            ArangoCursor<Map> q = db.query(query, null, options, Map.class);
            m.put("count", q.getCount());
            m.put("fullCount", q.getStats().getFullCount());
            m.put("data", q.asListRemaining());
        } catch (ArangoDBException e) {
            if (e.getResponseCode() == 404) {
                m.put("count", 0);
                m.put("fullCount", 0);
                m.put("data", new ArrayList<Map>());
            } else {
                throw e;
            }
        }
        return m;
    }

    public ReleaseStatusResponse getReleaseStatus(ArangoDocumentReference document, ArangoConnection inferredConnection, ArangoConnection releasedConnection) {
        String queryInferred = queryFactory.getOriginalIdOfDocumentWithChildren(document, inferredConnection);
        List<Map> inferredResult = inferredConnection.getOrCreateDB().query(queryInferred, null, new AqlQueryOptions(), Map.class).asListRemaining().stream().filter(Objects::nonNull).collect(Collectors.toList());
        if (inferredResult.isEmpty()) {
            return null;
        } else if (inferredResult.size() == 1) {
            String queryReleased = queryFactory.getOriginalIdOfDocumentWithChildren(document, releasedConnection);
            List<Map> releasedResult = releasedConnection.getOrCreateDB().query(queryReleased, null, new AqlQueryOptions(), Map.class).asListRemaining().stream().filter(Objects::nonNull).collect(Collectors.toList());
            ReleaseStatusResponse releaseStatusResponse = new ReleaseStatusResponse();
            Map inferred = inferredResult.get(0);
            Object inferredChildren = inferred.get("children");
            if (!(inferredChildren instanceof List)) {
                throw new UnexpectedResultStructure("Expected list for children");
            }
            if (releasedResult.isEmpty()) {
                releaseStatusResponse.setRootStatus(ReleaseStatus.NOT_RELEASED);
                List<String> releasedChildren = new ArrayList<>();
                Map<NexusSchemaReference, Set<String>> keysBySchema = ((List<String>) inferredChildren).stream().filter(Objects::nonNull).map(NexusInstanceReference::createFromUrl).collect(Collectors.groupingBy(c -> c.getNexusSchema(), Collectors.mapping(NexusInstanceReference::getId, Collectors.toSet())));
                for (NexusSchemaReference nexusSchemaReference : keysBySchema.keySet()) {
                    String queryChildren = queryFactory.getOriginalIds(ArangoCollectionReference.fromNexusSchemaReference(nexusSchemaReference), keysBySchema.get(nexusSchemaReference), releasedConnection);
                    List<Set> sets = releasedConnection.getOrCreateDB().query(queryChildren, null, new AqlQueryOptions(), Set.class).asListRemaining();
                    if (sets.isEmpty() || sets.size() > 1) {
                        throw new UnexpectedResultStructure(String.format("Unexpected number of elements when querying for originalIds: %d root elements found", sets.size()));
                    }
                    releasedChildren.addAll(sets.stream().filter(Objects::nonNull).map(s -> s.toString()).collect(Collectors.toSet()));
                }
                releaseStatusResponse.setChildrenStatus(findChildrenReleaseState((List<String>) inferredChildren, releasedChildren));
                return releaseStatusResponse;
            } else if (releasedResult.size() == 1) {
                Object inferredId = inferred.get("root");
                Map released = releasedResult.get(0);
                Object releasedId = released.get("root");
                if (inferredId == null) {
                    throw new UnexpectedResultStructure("Did not expect the result structure for root");
                }
                if(releasedId==null){
                    releaseStatusResponse.setRootStatus(ReleaseStatus.NOT_RELEASED);
                } else if (inferredId.equals(releasedId)) {
                    releaseStatusResponse.setRootStatus(ReleaseStatus.RELEASED);
                } else {
                    releaseStatusResponse.setRootStatus(ReleaseStatus.HAS_CHANGED);
                }
                Object releasedChildren = released.get("children");
                if (!(inferredChildren instanceof List && releasedChildren instanceof List)) {
                    throw new UnexpectedResultStructure("Expected list for children");
                }
                ReleaseStatus childrenReleaseState = findChildrenReleaseState((List<String>) inferredChildren, (List<String>) releasedChildren);
                releaseStatusResponse.setChildrenStatus(childrenReleaseState);
                return releaseStatusResponse;
            } else {
                throw new UnexpectedNumberOfResults(String.format("Too many results in query for id %s in inferred database", document.getId()));
            }
        }
        return null;
    }

    private ReleaseStatus findChildrenReleaseState
            (List<String> inferredChildren, List<String> releasedChildren) {
        inferredChildren.removeAll(releasedChildren);
        if (inferredChildren.isEmpty()) {
            return null;
        } else {
            List<String> inferredChildrenWithoutRevision = inferredChildren.stream().map(NexusInstanceReference::createFromUrl).filter(Objects::nonNull).map(c -> c.getRelativeUrl().getUrl()).collect(Collectors.toList());
            List<String> releasedChildrenWithoutRevision = releasedChildren.stream().map(NexusInstanceReference::createFromUrl).filter(Objects::nonNull).map(c -> c.getRelativeUrl().getUrl()).collect(Collectors.toList());
            inferredChildrenWithoutRevision.removeAll(releasedChildrenWithoutRevision);
            if (inferredChildrenWithoutRevision.isEmpty()) {
                return ReleaseStatus.HAS_CHANGED;
            } else {
                return ReleaseStatus.NOT_RELEASED;
            }
        }
    }

    public List<Map> getInstance(ArangoDocumentReference instanceReference, ArangoConnection driver){
        ArangoDatabase db = driver.getOrCreateDB();
        String query = queryFactory.getInstance(instanceReference);
        ArangoCursor<Map> q = db.query(query, null, new AqlQueryOptions(), Map.class);
        return q.asListRemaining();
    }


}
