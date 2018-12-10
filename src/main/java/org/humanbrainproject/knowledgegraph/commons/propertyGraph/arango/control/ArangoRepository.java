package org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control;

import com.arangodb.ArangoCollection;
import com.arangodb.ArangoCursor;
import com.arangodb.ArangoDBException;
import com.arangodb.ArangoDatabase;
import com.arangodb.entity.CollectionType;
import com.arangodb.model.AqlQueryOptions;
import com.arangodb.model.CollectionCreateOptions;
import com.github.jsonldjava.core.JsonLdConsts;
import org.apache.commons.lang.StringUtils;
import org.humanbrainproject.knowledgegraph.commons.authorization.control.AuthorizationController;
import org.humanbrainproject.knowledgegraph.commons.authorization.entity.Credential;
import org.humanbrainproject.knowledgegraph.commons.authorization.entity.InternalMasterKey;
import org.humanbrainproject.knowledgegraph.commons.jsonld.control.JsonTransformer;
import org.humanbrainproject.knowledgegraph.commons.labels.SemanticsToHumanTranslator;
import org.humanbrainproject.knowledgegraph.commons.nexus.control.NexusConfiguration;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.AuthorizedAccess;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.UnauthorizedAccess;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.query.ArangoQueryFactory;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoCollectionReference;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoDocumentReference;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.exceptions.UnexpectedNumberOfResults;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.entity.SubSpace;
import org.humanbrainproject.knowledgegraph.commons.vocabulary.ArangoVocabulary;
import org.humanbrainproject.knowledgegraph.commons.vocabulary.HBPVocabulary;
import org.humanbrainproject.knowledgegraph.commons.vocabulary.SchemaOrgVocabulary;
import org.humanbrainproject.knowledgegraph.indexing.control.MessageProcessor;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusInstanceReference;
import org.humanbrainproject.knowledgegraph.query.entity.JsonDocument;
import org.humanbrainproject.knowledgegraph.query.entity.QueryResult;
import org.humanbrainproject.knowledgegraph.releasing.entity.ReleaseStatus;
import org.humanbrainproject.knowledgegraph.releasing.entity.ReleaseStatusResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class ArangoRepository {

    @Autowired
    NexusConfiguration nexusConfiguration;
    protected Logger logger = LoggerFactory.getLogger(ArangoRepository.class);

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

    @Autowired
    SemanticsToHumanTranslator semanticsToHumanTranslator;

    @Autowired
    AuthorizationController authorizationController;

    @Autowired
    JsonTransformer jsonTransformer;


    @AuthorizedAccess("Although not sensitive, we would like to return references which are readable by the user only")
    public NexusInstanceReference findBySchemaOrgIdentifier(ArangoCollectionReference collectionReference, String value, Credential credential) {
        if (!databaseFactory.getDefaultDB().getOrCreateDB().collection(collectionReference.getName()).exists()) {
            return null;
        }
        String query = queryFactory.queryForValueWithProperty(SchemaOrgVocabulary.IDENTIFIER, value, Collections.singleton(collectionReference), ArangoVocabulary.NEXUS_RELATIVE_URL_WITH_REV, authorizationController.getReadableOrganizations(credential));
        List<List> result = query == null ? new ArrayList<>() : databaseFactory.getDefaultDB().getOrCreateDB().query(query, null, new AqlQueryOptions(), List.class).asListRemaining();
        if (result.size() == 1) {
            if (result.get(0) != null) {
                List list = (List) result.get(0);
                if (list.isEmpty()) {
                    return null;
                } else if (list.size() == 1) {
                    String url = (String) ((List) result.get(0)).get(0);
                    return url != null ? NexusInstanceReference.createFromUrl(url) : null;
                } else {
                    throw new UnexpectedNumberOfResults(String.format("Multiple instances with the same identifier in the same schema: %s", StringUtils.join(list.stream().filter(Objects::nonNull).map(Object::toString).toArray(), ", ")));
                }
            } else {
                return null;
            }
        }
        throw new UnexpectedNumberOfResults("The query for value with property should return a single item");
    }


    @AuthorizedAccess("Although not sensitive, we would like to return references which are readable by the user only")
    public Set<NexusInstanceReference> findOriginalIdsWithLinkTo(ArangoDocumentReference instanceReference, ArangoCollectionReference collectionReference, ArangoConnection arangoConnection, Credential credential) {
        Set<ArangoCollectionReference> collections = arangoConnection.getCollections();
        if (collections.contains(instanceReference.getCollection()) && collections.contains(collectionReference)) {
            String query = queryFactory.queryOriginalIdForLink(instanceReference, collectionReference, authorizationController.getReadableOrganizations(credential));
            List<String> ids = arangoConnection.getOrCreateDB().query(query, null, new AqlQueryOptions(), String.class).asListRemaining();
            return ids.stream().filter(Objects::nonNull).map(NexusInstanceReference::createFromUrl).collect(Collectors.toSet());
        }
        return Collections.emptySet();
    }


    private <T> T getDocumentByKey(ArangoDocumentReference document, Class<T> clazz, ArangoConnection connection, Credential credential) {
        Map doc = getDocument(document, connection, credential);
        if (doc != null) {
            if (clazz.isInstance(doc)) {
                return (T) doc;
            }
            return connection.getOrCreateDB().collection(document.getCollection().getName()).getDocument(document.getKey(), clazz);
        }
        return null;
    }

    public NexusInstanceReference findOriginalId(NexusInstanceReference reference, Credential credential) {
        Map byKey = null;
        for (SubSpace subSpace : SubSpace.values()) {
            if (byKey == null) {
                ArangoDocumentReference arangoDocumentReferenceInSubSpace = ArangoDocumentReference.fromNexusInstance(reference.toSubSpace(subSpace));
                byKey = getDocumentByKey(arangoDocumentReferenceInSubSpace, Map.class, databaseFactory.getDefaultDB(), credential);
            }
        }
        NexusInstanceReference result = reference.clone();
        if (byKey != null) {
            Object rev = byKey.get(ArangoVocabulary.NEXUS_REV);
            if (rev != null) {
                int revision = Integer.parseInt(rev.toString());
                if (result.getRevision() == null || result.getRevision() < revision) {
                    result.setRevision(revision);
                }
            }
            Object originalParent = byKey.get(HBPVocabulary.INFERENCE_EXTENDS);
            if (originalParent == null) {
                originalParent = byKey.get(HBPVocabulary.INFERENCE_OF);
            }
            NexusInstanceReference originalReference = null;
            if (originalParent instanceof Map) {
                String id = (String) ((Map) originalParent).get(JsonLdConsts.ID);
                originalReference = NexusInstanceReference.createFromUrl(id);
            } else if (byKey.get(JsonLdConsts.ID) != null) {
                originalReference = NexusInstanceReference.createFromUrl((String) byKey.get(JsonLdConsts.ID));
            }
            if (originalReference != null && !reference.isSameInstanceRegardlessOfRevision(originalReference)) {
                Map originalObject = getDocumentByKey(ArangoDocumentReference.fromNexusInstance(originalReference), Map.class, databaseFactory.getDefaultDB(), credential);
                if (originalObject != null) {
                    Object originalRev = originalObject.get(ArangoVocabulary.NEXUS_REV);
                    if (originalRev != null) {
                        originalReference.setRevision(Integer.parseInt(originalRev.toString()));
                    }
                }
                result = originalReference;
            }
        }
        return result;
    }

    public Integer getCurrentRevision(ArangoDocumentReference documentReference) {
        Map document = getDocument(documentReference, databaseFactory.getDefaultDB(), new InternalMasterKey());
        if (document != null) {
            Object rev = document.get(ArangoVocabulary.NEXUS_RELATIVE_URL_WITH_REV);
            if (rev != null) {
                return Integer.parseInt(rev.toString());
            }
        }
        return null;
    }

    @AuthorizedAccess
    public Map getDocument(ArangoDocumentReference documentReference, ArangoConnection arangoConnection, Credential credential) {
        Map document = arangoConnection.getOrCreateDB().getDocument(documentReference.getId(), Map.class);
        if (document != null && authorizationController.isReadable(document, credential)) {
            return document;
        }
        return null;
    }

    public String getPayloadById(ArangoDocumentReference documentReference, ArangoConnection arangoConnection, Credential credential) {
        return getDocumentByKey(documentReference, String.class, arangoConnection, credential);
    }

    public Set<ArangoDocumentReference> getReferencesBelongingToInstance(NexusInstanceReference nexusInstanceReference, ArangoConnection arangoConnection, Credential credential) {
        Set<ArangoCollectionReference> collections = new HashSet<>(arangoConnection.getEdgesCollectionNames());
        String query = queryFactory.queryForIdsWithProperty(ArangoVocabulary.NEXUS_RELATIVE_URL_WITH_REV, nexusInstanceReference.getFullId(true), collections, authorizationController.getReadableOrganizations(credential));
        List<List> result = query == null ? new ArrayList<>() : arangoConnection.getOrCreateDB().query(query, null, new AqlQueryOptions(), List.class).asListRemaining();
        if (result.size() == 1) {
            return ((List<String>) result.get(0)).stream().filter(Objects::nonNull).map(id -> ArangoDocumentReference.fromId(id.toString())).collect(Collectors.toSet());
        }
        return new LinkedHashSet<>();
    }

    public List<Map> inDepthGraph(ArangoDocumentReference document, Integer step, ArangoConnection connection, Credential credential) {
        ArangoDatabase db = connection.getOrCreateDB();
        String query = queryFactory.queryInDepthGraph(connection.getEdgesCollectionNames(), document, step, authorizationController.getReadableOrganizations(credential));
        try {
            ArangoCursor<Map> q = db.query(query, null, new AqlQueryOptions(), Map.class);
            return q.asListRemaining();
        } catch (ArangoDBException e) {
            logger.error("Arango query exception - {}", query);
            throw e;
        }
    }


    private Map interpretMap(Map map) {
        Object name = map.get(SchemaOrgVocabulary.NAME);
        Object identifier = map.get(SchemaOrgVocabulary.IDENTIFIER);
        if (name != null) {
            map.put("label", name);
        } else {
            map.put("label", identifier);
        }
        if (map.containsKey(JsonLdConsts.TYPE)) {
            Object types = map.get(JsonLdConsts.TYPE);
            Object relevantType = null;
            if (types instanceof List && !((List) types).isEmpty()) {
                relevantType = ((List) types).get(0);
            } else {
                relevantType = types;
            }
            if (relevantType != null) {
                map.put("type", semanticsToHumanTranslator.translateSemanticValueToHumanReadableLabel(relevantType.toString()));
            }
        }
        if (map.containsKey(JsonLdConsts.ID)) {
            NexusInstanceReference fromUrl = NexusInstanceReference.createFromUrl((String) map.get(JsonLdConsts.ID));
            if (fromUrl != null) {
                map.put("relativeUrl", fromUrl.getRelativeUrl().getUrl());
            }
        }

        Object linkType = map.get("linkType");
        if (linkType != null) {
            map.put("linkType", semanticsToHumanTranslator.translateSemanticValueToHumanReadableLabel((String) linkType));
        }

        for (Object key : map.keySet()) {
            if (map.get(key) instanceof Map) {
                interpretMap((Map) map.get(key));
            } else if (map.get(key) instanceof Collection) {
                for (Object o : ((Collection) map.get(key))) {
                    if (o instanceof Map) {
                        interpretMap((Map) o);
                    }
                }
            }
        }
        return map;

    }


    @AuthorizedAccess
    public Map getReleaseGraph(ArangoDocumentReference document, Optional<Integer> maxDepth, Credential credential) {
        ArangoDatabase db = databaseFactory.getInferredDB().getOrCreateDB();
        //Ensure the release-collection exists
        Set<ArangoCollectionReference> edgesCollectionNames = databaseFactory.getInferredDB().getEdgesCollectionNames();
        String releaseInstanceEdgeCollection = ArangoCollectionReference.fromFieldName(HBPVocabulary.RELEASE_INSTANCE).getName();
        if (!db.collection(releaseInstanceEdgeCollection).exists()) {
            db.createCollection(releaseInstanceEdgeCollection, new CollectionCreateOptions().type(CollectionType.EDGES));
        }
        String query = queryFactory.queryReleaseGraph(edgesCollectionNames, document, maxDepth.orElse(6), authorizationController.getReadableOrganizations(credential));
        ArangoCursor<Map> q = db.query(query, null, new AqlQueryOptions(), Map.class);
        List<Map> results = q.asListRemaining().stream().filter(Objects::nonNull).collect(Collectors.toList());
        if (results.size() > 1) {
            throw new UnexpectedNumberOfResults("The release graph query should only return a single document since it is based on an id");
        }
        return !results.isEmpty() ? interpretMap(results.get(0)) : null;
    }

    @AuthorizedAccess
    public QueryResult<List<Map>> getInstances(ArangoCollectionReference collection, Integer from, Integer size, String searchTerm, ArangoConnection driver, Credential credential) {
        ArangoDatabase db = driver.getOrCreateDB();
        QueryResult<List<Map>> result = new QueryResult<>();
        String query = queryFactory.getInstanceList(collection, from, size, searchTerm, authorizationController.getReadableOrganizations(credential), false);
        AqlQueryOptions options = new AqlQueryOptions();
        if (size != null) {
            options.fullCount(true);
        } else {
            options.count(true);
        }
        try {
            ArangoCursor<Map> cursor = db.query(query, null, options, Map.class);
            Long count;
            if (size != null) {
                count = cursor.getStats().getFullCount();
            } else {
                count = cursor.getCount().longValue();
            }
            result.setResults(cursor.asListRemaining().stream().map(l -> new JsonDocument(l).removeAllInternalKeys()).collect(Collectors.toList()));
            result.setTotal(count);
            result.setSize(size == null ? count : size);
            result.setStart(from != null ? from : 0L);
        } catch (ArangoDBException e) {
            if (e.getResponseCode() == 404) {
                result.setSize(0L);
                result.setTotal(0L);
                result.setResults(Collections.emptyList());
                result.setStart(0L);
            } else {
                throw e;
            }
        }
        return result;
    }


    /**
     * Use getInstances instead to ensure a unified response structure
     *
     * @param collection
     * @param from
     * @param size
     * @param searchTerm
     * @param driver
     * @param credential
     * @return
     */
    @Deprecated
    @AuthorizedAccess
    public Map getInstanceList(ArangoCollectionReference collection, Integer from, Integer size, String searchTerm, ArangoConnection driver, Credential credential) {
        ArangoDatabase db = driver.getOrCreateDB();
        String query = queryFactory.getInstanceList(collection, from, size, searchTerm, authorizationController.getReadableOrganizations(credential), true);
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

    ReleaseStatus findWorstReleaseStatusOfChildren(Map map, ReleaseStatus currentStatus, boolean isRoot) {
        ReleaseStatus worstStatusSoFar = currentStatus;
        if (map != null) {
            //Skip status of root instance
            if (!isRoot) {
                try {
                    Object status = map.get("status");
                    if (status instanceof String) {
                        ReleaseStatus releaseStatus = ReleaseStatus.valueOf((String) status);
                        if (releaseStatus.isWorst()) {
                            return releaseStatus;
                        }
                        if (releaseStatus.isWorseThan(worstStatusSoFar)) {
                            worstStatusSoFar = releaseStatus;
                        }
                    }
                } catch (IllegalArgumentException e) {
                    logger.error(String.format("Was not able to parse the status with the value %s", map.get("status")));
                }
            }

            Object children = map.get("children");
            if (children instanceof List) {
                for (Object o : ((List) children)) {
                    if (o instanceof Map) {
                        worstStatusSoFar = findWorstReleaseStatusOfChildren((Map) o, worstStatusSoFar, false);
                        if (worstStatusSoFar.isWorst()) {
                            return worstStatusSoFar;
                        }
                    }
                }
            }


        }
        return worstStatusSoFar;
    }


    @AuthorizedAccess
    public ReleaseStatusResponse getReleaseStatus(ArangoDocumentReference document, Credential credential) {
        Map releaseGraph = getReleaseGraph(document, Optional.empty(), credential);
        if (releaseGraph != null) {
            ReleaseStatusResponse response = new ReleaseStatusResponse();
            response.setRootStatus(ReleaseStatus.valueOf((String) releaseGraph.get("status")));
            response.setChildrenStatus(findWorstReleaseStatusOfChildren(releaseGraph, null, true));
            return response;
        }
        return null;
    }


    @AuthorizedAccess
    public JsonDocument getInstance(ArangoDocumentReference instanceReference, ArangoConnection driver, Credential credential) {
        ArangoDatabase db = driver.getOrCreateDB();
        ArangoCollection collection = db.collection(instanceReference.getCollection().getName());
        if (collection.exists() && collection.documentExists(instanceReference.getKey())) {
            JsonDocument jsonDocument = new JsonDocument(collection.getDocument(instanceReference.getKey(), Map.class));
            boolean readable = authorizationController.isReadable(jsonDocument, credential);
            if (readable) {
                return jsonDocument;
            }
            //TODO shall we silently return null if there is no read access?
            return null;
        }
        return null;
    }

    @AuthorizedAccess
    public Map getBookmarks(NexusInstanceReference document, Integer from, Integer size, String
            searchTerm, ArangoConnection driver, Credential credential) {
        ArangoDatabase db = driver.getOrCreateDB();
        String query = queryFactory.getBookmarks(document, from, size, searchTerm, authorizationController.getReadableOrganizations(credential));
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


    @UnauthorizedAccess("The internal documents are open to everyone (although exposed through internal APIs only")
    public List<Map> getInternalDocuments(ArangoCollectionReference collection) {
        ArangoDatabase db = databaseFactory.getInternalDB().getOrCreateDB();
        String query = queryFactory.getAllInternalDocumentsOfACollection(collection);
        ArangoCursor<Map> q = db.query(query, null, new AqlQueryOptions(), Map.class);
        return q.asListRemaining();
    }

    @UnauthorizedAccess("The internal documents are open to everyone (although exposed through internal APIs only")
    public <T> T getInternalDocumentByKey(ArangoDocumentReference document, Class<T> clazz) {
        return databaseFactory.getInternalDB().getOrCreateDB().collection(document.getCollection().getName()).getDocument(document.getKey(), clazz);
    }

    @UnauthorizedAccess("Querying the data structure is public knowledge - there is no data exposed")
    public List<Map> getAttributesWithCount(ArangoCollectionReference reference) {
        ArangoDatabase db = databaseFactory.getInferredDB().getOrCreateDB();
        if (db.collection(reference.getName()).exists()) {
            String q = queryFactory.getAttributesWithCount(reference);
            ArangoCursor<Map> result = db.query(q, null, new AqlQueryOptions(), Map.class);
            return result.asListRemaining();
        } else {
            return Collections.emptyList();
        }
    }


    public List<Map> getInboundRelationsForDocument(ArangoDocumentReference documentReference) {
        ArangoConnection inferredDB = databaseFactory.getInferredDB();
        Set<ArangoCollectionReference> edgesCollectionNames = inferredDB.getEdgesCollectionNames();
        String q = queryFactory.queryInboundRelationsForDocument(documentReference, edgesCollectionNames, authorizationController.getReadableOrganizations(new InternalMasterKey()));
        //ATTENTION: Use of internal master-key! Ensure only meta-data is leaving this method!
        ArangoCursor<Map> result = inferredDB.getOrCreateDB().query(q, null, new AqlQueryOptions(), Map.class);
        return result.asListRemaining();
    }

    @UnauthorizedAccess("This is a query about the structure and therefore is not protected")
    public List<Map> getDirectRelationsWithType(ArangoCollectionReference collectionReference, boolean outbound){
        ArangoConnection inferredDB = databaseFactory.getInferredDB();
        if(inferredDB.getOrCreateDB().collection(collectionReference.getName()).exists()) {
            Set<ArangoCollectionReference> edgesCollectionNames = inferredDB.getEdgesCollectionNames();
            String q = queryFactory.queryDirectRelationsWithType(collectionReference, edgesCollectionNames, outbound);
            ArangoCursor<Map> result = inferredDB.getOrCreateDB().query(q, null, new AqlQueryOptions(), Map.class);
            return result.asListRemaining();
        }
        return Collections.emptyList();
    }



    public Set<ArangoCollectionReference> getCollectionNames() {
        ArangoConnection inferredDB = databaseFactory.getInferredDB();
        return inferredDB.getEdgesCollectionNames();
    }

}
