package org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control;

import com.arangodb.ArangoCollection;
import com.arangodb.ArangoCursor;
import com.arangodb.ArangoDBException;
import com.arangodb.ArangoDatabase;
import com.arangodb.model.AqlQueryOptions;
import org.humanbrainproject.knowledgegraph.annotations.ToBeTested;
import org.humanbrainproject.knowledgegraph.commons.authorization.control.AuthorizationContext;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.AuthorizedAccess;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.query.ArangoQueryFactory;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoCollectionReference;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoDocumentReference;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.exceptions.UnexpectedNumberOfResults;
import org.humanbrainproject.knowledgegraph.commons.vocabulary.ArangoVocabulary;
import org.humanbrainproject.knowledgegraph.indexing.control.PID;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusInstanceReference;
import org.humanbrainproject.knowledgegraph.query.entity.JsonDocument;
import org.humanbrainproject.knowledgegraph.query.entity.QueryResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
@ToBeTested(systemTestRequired = false)
public class ArangoRepository {

    protected Logger logger = LoggerFactory.getLogger(ArangoRepository.class);

    @Autowired
    AuthorizationContext authorizationContext;

    @Autowired
    ArangoQueryFactory queryFactory;

    public Map findPid(String type, Set<String> identifiers, ArangoConnection connection) {
        ArangoDatabase db = connection.getOrCreateDB();
        ArangoCollection collection = db.collection(ArangoCollectionReference.fromNexusSchemaReference(PID.PID_SCHEMA_REFERENCE).getName());
        if (collection.exists()) {
            String query = queryFactory.queryByIdentifierArrayAndType(PID.PID_SCHEMA_REFERENCE, authorizationContext.getReadableOrganizations());
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("identifiers", identifiers);
            parameters.put("type", type);
            List<Map> results = db.query(query, parameters, new AqlQueryOptions(), Map.class).asListRemaining();
            if (results == null || results.isEmpty()) {
                return null;
            }
            if (results.size() == 1) {
                return results.get(0);
            }
            throw new UnexpectedNumberOfResults(String.format("Expected no or one results for PID query - there were %d results", results.size()));
        } else {
            return null;
        }
    }


    <T> T getDocumentByKey(ArangoDocumentReference document, Class<T> clazz, ArangoConnection connection) {
        Map doc = getDocument(document, connection);
        if (doc != null) {
            if (clazz.isInstance(doc)) {
                return (T) doc;
            }
            return connection.getOrCreateDB().collection(document.getCollection().getName()).getDocument(document.getKey(), clazz);
        }
        return null;
    }


    public Map getDocument(ArangoDocumentReference documentReference, ArangoConnection arangoConnection) {
        Map document = arangoConnection.getOrCreateDB().getDocument(documentReference.getId(), Map.class);
        if (document != null && authorizationContext.isReadable(document)) {
            return document;
        }
        return null;
    }

    public String getPayloadById(ArangoDocumentReference documentReference, ArangoConnection arangoConnection) {
        return getDocumentByKey(documentReference, String.class, arangoConnection);
    }

    public Set<ArangoDocumentReference> getReferencesBelongingToInstance(NexusInstanceReference nexusInstanceReference, ArangoConnection arangoConnection) {
        Set<ArangoCollectionReference> collections = new HashSet<>(arangoConnection.getEdgesCollectionNames());
        String query = queryFactory.queryForIdsWithProperty(ArangoVocabulary.NEXUS_RELATIVE_URL_WITH_REV, nexusInstanceReference.getFullId(true), collections, authorizationContext.getReadableOrganizations());
        List<List> result = query == null ? new ArrayList<>() : arangoConnection.getOrCreateDB().query(query, null, new AqlQueryOptions(), List.class).asListRemaining();
        if (result.size() == 1) {
            return ((List<String>) result.get(0)).stream().filter(Objects::nonNull).map(id -> ArangoDocumentReference.fromId(id.toString())).collect(Collectors.toSet());
        }
        return new LinkedHashSet<>();
    }

    public List<Map> inDepthGraph(ArangoDocumentReference document, Integer step, ArangoConnection connection) {
        ArangoDatabase db = connection.getOrCreateDB();
        String query = queryFactory.queryInDepthGraph(connection.getEdgesCollectionNames(), document, step, authorizationContext.getReadableOrganizations());
        try {
            ArangoCursor<Map> q = db.query(query, null, new AqlQueryOptions(), Map.class);
            return q.asListRemaining();
        } catch (ArangoDBException e) {
            logger.error("Arango query exception - {}", query);
            throw e;
        }
    }


    public QueryResult<List<Map>> getInstances(ArangoCollectionReference collection, Integer from, Integer size, String searchTerm, ArangoConnection arangoConnection) {
        QueryResult<List<Map>> result = new QueryResult<>();
        String query = queryFactory.getInstanceList(collection, from, size, searchTerm, authorizationContext.getReadableOrganizations(), false);
        AqlQueryOptions options = new AqlQueryOptions();
        if (size != null) {
            options.fullCount(true);
        } else {
            options.count(true);
        }
        try {
            ArangoCursor<Map> cursor = arangoConnection.getOrCreateDB().query(query, null, options, Map.class);
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


    @AuthorizedAccess
    public List<Map> getLinkingInstances(ArangoDocumentReference fromInstance, ArangoDocumentReference toInstance, ArangoCollectionReference reference, ArangoConnection driver) {
        ArangoDatabase db = driver.getOrCreateDB();
        String query = queryFactory.queryLinkingInstanceBetweenVertices(fromInstance, toInstance, reference, authorizationContext.getReadableOrganizations());
        ArangoCursor<Map> q = db.query(query, null, new AqlQueryOptions(), Map.class);
        return q.asListRemaining();
    }


    @AuthorizedAccess
    public JsonDocument getInstance(ArangoDocumentReference instanceReference, ArangoConnection driver) {
        ArangoDatabase db = driver.getOrCreateDB();
        ArangoCollection collection = db.collection(instanceReference.getCollection().getName());
        if (collection.exists() && collection.documentExists(instanceReference.getKey())) {
            JsonDocument jsonDocument = new JsonDocument(collection.getDocument(instanceReference.getKey(), Map.class));
            boolean readable = authorizationContext.isReadable(jsonDocument);
            if (readable) {
                return jsonDocument;
            }
            //TODO shall we silently return null if there is no read access?
            return null;
        }
        return null;
    }

}
