package org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control;

import com.arangodb.ArangoCollection;
import com.arangodb.ArangoCursor;
import com.arangodb.ArangoDBException;
import com.arangodb.ArangoDatabase;
import com.arangodb.model.AqlQueryOptions;
import org.humanbrainproject.knowledgegraph.annotations.ToBeTested;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.UnauthorizedAccess;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.query.ArangoQueryFactory;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoCollectionReference;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoDocumentReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@ToBeTested(systemTestRequired = true)
@UnauthorizedAccess("The internal documents are open to everyone (although exposed through internal APIs only")
public class ArangoInternalRepository {

    @Autowired
    ArangoDatabaseFactory databaseFactory;

    @Autowired
    ArangoQueryFactory queryFactory;


    protected Logger logger = LoggerFactory.getLogger(ArangoRepository.class);

    private ArangoDatabase getDB(){
        return databaseFactory.getInternalDB().getOrCreateDB();
    }

    public void insertOrUpdateDocument(ArangoDocumentReference document, String documentPayload) {
        //TODO ensure authorization
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
                    logger.debug("Payload of document {} in database {}: {}", document.getId(), db.name(), documentPayload);
                } catch (ArangoDBException dbexception) {
                    logger.error(String.format("Was not able to update document: %s in database %s", document.getId(), db.name()), dbexception);
                    throw dbexception;
                }
                collection.updateDocument(document.getKey(), documentPayload);
            } else {
                try {
                    collection.insertDocument(documentPayload);
                    logger.info("Inserted document: {} in database {}", document.getId(), db.name());
                    logger.debug("Payload of document {} in database {}: {}", document.getId(), db.name(), documentPayload);
                } catch (ArangoDBException dbexception) {
                    logger.error(String.format("Was not able to insert document: %s in database %s", document.getId(), db.name()), dbexception);
                    throw dbexception;
                }
            }
        }
    }

    public <T> List<T> getAll(ArangoCollectionReference collection, Class<T> clazz) {
        String query = queryFactory.getAll(collection);
        try {
            return getDB().query(query, null, new AqlQueryOptions(), clazz).asListRemaining();
        } catch (ArangoDBException e) {
            logger.error("Arango query exception - {}", query);
            throw e;
        }
    }

    public List<Map> getInternalDocuments(ArangoCollectionReference collection) {
        String query = queryFactory.getAllInternalDocumentsOfACollection(collection);
        ArangoCursor<Map> q = getDB().query(query, null, new AqlQueryOptions(), Map.class);
        return q.asListRemaining();
    }

    public <T> List<T> getInternalDocumentsWithKeyPrefix(ArangoCollectionReference collection, String keyPrefix, Class<T> returnType) {
        String query = queryFactory.getInternalDocumentsOfCollectionWithKeyPrefix(collection, keyPrefix);
        ArangoCursor<T> q = getDB().query(query, null, new AqlQueryOptions(), returnType);
        return q.asListRemaining();
    }

    public <T> T getInternalDocumentByKey(ArangoDocumentReference document, Class<T> clazz) {
        return getDB().collection(document.getCollection().getName()).getDocument(document.getKey(), clazz);
    }


    public boolean doesDocumentExist(ArangoCollectionReference collectionReference, String key) {
        ArangoDocumentReference documentReference = new ArangoDocumentReference(collectionReference, key);
        ArangoCollection collection = databaseFactory.getInternalDB().getOrCreateDB().collection(documentReference.getCollection().getName());
        if (collection.exists()) {
            return collection.documentExists(documentReference.getKey());
        }
        return false;
    }
}
