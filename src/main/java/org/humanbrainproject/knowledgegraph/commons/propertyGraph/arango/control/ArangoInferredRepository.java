package org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control;

import com.arangodb.ArangoCollection;
import com.arangodb.ArangoCursor;
import com.arangodb.ArangoDBException;
import com.arangodb.model.AqlQueryOptions;
import org.humanbrainproject.knowledgegraph.annotations.ToBeTested;
import org.humanbrainproject.knowledgegraph.commons.authorization.control.AuthorizationContext;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.AuthorizedAccess;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.query.ArangoQueryFactory;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoCollectionReference;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusInstanceReference;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusSchemaReference;
import org.humanbrainproject.knowledgegraph.query.entity.Pagination;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@ToBeTested(systemTestRequired = true)
public class ArangoInferredRepository {

    @Autowired
    ArangoDatabaseFactory databaseFactory;

    @Autowired
    ArangoQueryFactory queryFactory;

    @Autowired
    AuthorizationContext authorizationContext;


    protected Logger logger = LoggerFactory.getLogger(ArangoRepository.class);


    public Set<ArangoCollectionReference> getCollectionNames() {
        ArangoConnection inferredDB = databaseFactory.getInferredDB();
        return inferredDB.getEdgesCollectionNames();
    }

    public boolean hasInstances(ArangoCollectionReference reference){
        ArangoCollection collection = databaseFactory.getInferredDB().getOrCreateDB().collection(reference.getName());
        boolean exists = collection.exists();
        if(!exists){
            return false;
        }
        return collection.count().getCount()>0;

    }

    /**
     * Use getInstances instead to ensure a unified response structure
     */
    @Deprecated
    @AuthorizedAccess
    public Map getInstanceList(ArangoCollectionReference collection, String searchTerm, Pagination pagination) {
        String query = queryFactory.getInstanceList(collection, pagination!=null ? pagination.getStart() : null, pagination!=null ? pagination.getSize() : null, searchTerm, authorizationContext.getReadableOrganizations(), true);
        AqlQueryOptions options = new AqlQueryOptions().count(true).fullCount(true);
        Map m = new HashMap();
        try {
            ArangoCursor<Map> q = databaseFactory.getInferredDB().getOrCreateDB().query(query, null, options, Map.class);
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


    @AuthorizedAccess
    public Map getBookmarks(NexusInstanceReference document, String searchTerm, Pagination pagination) {
        String query = queryFactory.getBookmarks(document, pagination!=null ? pagination.getStart() : null, pagination!=null ? pagination.getSize() : null, searchTerm, authorizationContext.getReadableOrganizations());
        AqlQueryOptions options = new AqlQueryOptions().count(true).fullCount(true);
        Map m = new HashMap();
        try {
            ArangoCursor<Map> q = databaseFactory.getInferredDB().getOrCreateDB().query(query, null, options, Map.class);
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

    @AuthorizedAccess
    public List<Map> getSuggestionsByField(NexusSchemaReference schemaReference, String fieldName, String searchTerm, Pagination pagination){
        String query = queryFactory.querySuggestionByField(ArangoCollectionReference.fromNexusSchemaReference(schemaReference), ArangoCollectionReference.fromFieldName(fieldName), searchTerm, pagination != null ? pagination.getStart() : null, pagination != null ? pagination.getSize() : null, authorizationContext.getReadableOrganizations());
        ArangoCursor<Map> result = databaseFactory.getInferredDB().getOrCreateDB().query(query, null, new AqlQueryOptions(), Map.class);
        return result.asListRemaining();
    }

    @AuthorizedAccess
    public List<Map> getSuggestionsByType(NexusSchemaReference schemaReference, String fieldName, String searchTerm, Pagination pagination){
        String query = queryFactory.querySuggestionByField(ArangoCollectionReference.fromNexusSchemaReference(schemaReference), ArangoCollectionReference.fromFieldName(fieldName), searchTerm, pagination != null ? pagination.getStart() : null, pagination != null ? pagination.getSize() : null, authorizationContext.getReadableOrganizations());
        ArangoCursor<Map> result = databaseFactory.getInferredDB().getOrCreateDB().query(query, null, new AqlQueryOptions(), Map.class);
        return result.asListRemaining();
    }

}
