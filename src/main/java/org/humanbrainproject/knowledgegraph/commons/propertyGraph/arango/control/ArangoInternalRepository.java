package org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control;

import com.arangodb.ArangoCollection;
import com.arangodb.ArangoDBException;
import com.arangodb.ArangoDatabase;
import com.arangodb.model.AqlQueryOptions;
import org.humanbrainproject.knowledgegraph.commons.authorization.control.AuthorizationController;
import org.humanbrainproject.knowledgegraph.commons.authorization.control.SystemOidcClient;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.query.ArangoQueryFactory;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoCollectionReference;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoDocumentReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ArangoInternalRepository {

    @Autowired
    ArangoDatabaseFactory databaseFactory;

    @Autowired
    ArangoQueryFactory queryFactory;

    @Autowired
    SystemOidcClient systemOidcClient;

    @Autowired
    AuthorizationController authorizationController;

    protected Logger logger = LoggerFactory.getLogger(ArangoRepository.class);

    private ArangoDatabase getDB(){
        return databaseFactory.getInternalDB().getOrCreateDB();
    }

    public void insertOrUpdateDocument(ArangoDocumentReference document, String documentPayload) {
        if (document != null && documentPayload != null) {
            ArangoDatabase db = getDB();
            ArangoCollection collection = db.collection(document.getCollection().getName());
            if (!collection.exists()) {
                db.createCollection(document.getCollection().getName());
                logger.info("Created collection {} in database {}", document.getCollection().getName(), db.name());
                collection = db.collection(document.getCollection().getName());
            }
            if (collection.documentExists(document.getKey())) {
                try {
                    collection.updateDocument(document.getKey(), documentPayload);
                    logger.info("Updated document: {} in database {}", document.getId(), db.name());
                } catch (ArangoDBException dbexception) {
                    logger.error(String.format("Was not able to update document: %s in database %s", document.getId(), db.name()), dbexception);
                    throw dbexception;
                }
                collection.updateDocument(document.getKey(), documentPayload);
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

    public <T> List<T> getAll(ArangoCollectionReference collection, Class<T> clazz) {
        String query = queryFactory.getAll(collection, authorizationController.getReadableOrganizations(systemOidcClient.getAuthorizationToken()));
        try {
            return getDB().query(query, null, new AqlQueryOptions(), clazz).asListRemaining();
        } catch (ArangoDBException e) {
            logger.error("Arango query exception - {}", query);
            throw e;
        }
    }
}
