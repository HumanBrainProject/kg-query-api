package org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control;

import com.arangodb.ArangoCursor;
import com.arangodb.ArangoDatabase;
import com.arangodb.model.AqlQueryOptions;
import org.humanbrainproject.knowledgegraph.annotations.ToBeTested;
import org.humanbrainproject.knowledgegraph.commons.authorization.control.AuthorizationContext;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.UnauthorizedAccess;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.query.ArangoQueryFactory;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoCollectionReference;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoDocumentReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
@ToBeTested(systemTestRequired = true)
@UnauthorizedAccess("Querying the data structure is public knowledge - there is no data exposed")
public class ArangoStructureRepository {

    @Autowired
    ArangoDatabaseFactory databaseFactory;

    @Autowired
    ArangoQueryFactory queryFactory;

    @Autowired
    AuthorizationContext authorizationContext;


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
        String q = queryFactory.queryInboundRelationsForDocument(documentReference, edgesCollectionNames, authorizationContext.getReadableOrganizations());
        ArangoCursor<Map> result = inferredDB.getOrCreateDB().query(q, null, new AqlQueryOptions(), Map.class);
        return result.asListRemaining();
    }

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
}
