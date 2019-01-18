package org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control;

import com.arangodb.ArangoCollection;
import com.arangodb.ArangoCursor;
import com.arangodb.ArangoDBException;
import com.arangodb.ArangoDatabase;
import com.arangodb.entity.CollectionEntity;
import com.arangodb.entity.CollectionType;
import com.arangodb.model.AqlQueryOptions;
import com.arangodb.model.CollectionCreateOptions;
import org.humanbrainproject.knowledgegraph.annotations.ToBeTested;
import org.humanbrainproject.knowledgegraph.commons.authorization.control.AuthorizationContext;
import org.humanbrainproject.knowledgegraph.commons.authorization.control.AuthorizationController;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.query.ArangoQueryFactory;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoCollectionReference;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoDocumentReference;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.control.DatabaseTransaction;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.entity.Edge;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.entity.Vertex;
import org.humanbrainproject.knowledgegraph.indexing.entity.knownSemantics.LinkingInstance;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusInstanceReference;
import org.humanbrainproject.knowledgegraph.indexing.entity.todo.DeleteTodoItem;
import org.humanbrainproject.knowledgegraph.indexing.entity.todo.InsertOrUpdateInPrimaryStoreTodoItem;
import org.humanbrainproject.knowledgegraph.indexing.entity.todo.InsertTodoItem;
import org.humanbrainproject.knowledgegraph.indexing.entity.todo.TodoList;
import org.humanbrainproject.knowledgegraph.instances.control.InstanceManipulationController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;

@Component
@ToBeTested(integrationTestRequired = true, systemTestRequired = true)
public class ArangoTransaction implements DatabaseTransaction {

    @Autowired
    ArangoDocumentConverter arangoDocumentConverter;

    @Autowired
    AuthorizationController authorizationController;

    @Autowired
    InstanceManipulationController manipulationController;

    @Autowired
    ArangoQueryFactory queryFactory;

    @Autowired
    AuthorizationContext authorizationContext;


    protected Logger logger = LoggerFactory.getLogger(ArangoTransaction.class);

    @Override
    public void execute(TodoList todoList) {
        //First remove instances
        List<DeleteTodoItem> deleteItems = todoList.getDeleteTodoItems();
        for (DeleteTodoItem deleteItem : deleteItems) {
            ArangoConnection databaseConnection = deleteItem.getDatabaseConnection(ArangoConnection.class);
            if(databaseConnection!=null) {
                ArangoDatabase database = databaseConnection.getOrCreateDB();
                ArangoDocumentReference reference = ArangoDocumentReference.fromNexusInstance(deleteItem.getReference());
                deleteOutgoingRelations(reference, databaseConnection);
                deleteDocument(reference, database);
            }
        }

        //then add new instances
        List<InsertTodoItem> insertItems = todoList.getInsertTodoItems();
        for (InsertTodoItem insertItem : insertItems) {
            ArangoConnection databaseConnection = insertItem.getDatabaseConnection(ArangoConnection.class);
            if(databaseConnection!=null) {
                ArangoDatabase database = databaseConnection.getOrCreateDB();
                Vertex vertex = insertItem.getVertex();
                ArangoDocumentReference reference = ArangoDocumentReference.fromNexusInstance(vertex.getInstanceReference());
                LinkingInstance linkingInstance = new LinkingInstance(vertex.getQualifiedIndexingMessage());
                if(linkingInstance.isInstance()) {
                    ArangoDocumentReference documentReference = ArangoDocumentReference.fromNexusInstance(vertex.getInstanceReference());
                    deleteDocument(documentReference, database);
                    if(linkingInstance.getFrom()!=null && linkingInstance.getTo()!=null) {
                        String jsonFromLinkingInstance = arangoDocumentConverter.createJsonFromLinkingInstance(documentReference, linkingInstance.getFrom(), linkingInstance.getTo(), vertex.getInstanceReference(), vertex);
                        insertDocument(documentReference, jsonFromLinkingInstance, CollectionType.EDGES, database);
                    }
                }
                else {
                    //Remove already existing instances
                    deleteOutgoingRelations(reference, databaseConnection);
                    deleteDocument(reference, database);

                    String vertexJson = arangoDocumentConverter.createJsonFromVertex(reference, vertex, insertItem.getBlacklist());
                    if (vertexJson != null) {
                        insertDocument(reference, vertexJson, CollectionType.DOCUMENT, database);
                    }
                    for (Edge edge : vertex.getEdges()) {
                        ArangoDocumentReference document = ArangoDocumentReference.fromEdge(edge);
                        String jsonFromEdge = arangoDocumentConverter.createJsonFromEdge(document, vertex, edge, insertItem.getBlacklist());
                        insertDocument(document, jsonFromEdge, CollectionType.EDGES, database);
                        ArangoCollectionReference collection = ArangoCollectionReference.fromNexusSchemaReference(edge.getReference().getNexusSchema());
                        if (!database.collection(collection.getName()).exists()) {
                            database.createCollection(collection.getName(), new CollectionCreateOptions().type(CollectionType.DOCUMENT));
                        }
                    }
                }
            }
        }

        //and finally trigger primary store insertions/updates.
        List<InsertOrUpdateInPrimaryStoreTodoItem> insertOrUpdateInPrimaryStoreItems = todoList.getInsertOrUpdateInPrimaryStoreTodoItems();
        for (InsertOrUpdateInPrimaryStoreTodoItem insertOrUpdateInPrimaryStoreItem : insertOrUpdateInPrimaryStoreItems) {
            Vertex vertex = insertOrUpdateInPrimaryStoreItem.getVertex();
            NexusInstanceReference newReference = manipulationController.createInstanceByNexusIdAsSystemUser(vertex.getInstanceReference().getNexusSchema(), vertex.getInstanceReference().getId(), null, new LinkedHashMap(vertex.getQualifiedIndexingMessage().getQualifiedMap()), null);
            vertex.setInstanceReference(newReference);
        }
    }


    private void insertDocument(ArangoDocumentReference document, String documentPayload, CollectionType
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
                    logger.debug("Payload of document {} in database {}: {}", document.getId(), db.name(), documentPayload);
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
                    logger.debug("Payload of document {} in database {}: {}", document.getId(), db.name(), documentPayload);
                } catch (ArangoDBException dbexception) {
                    logger.error(String.format("Was not able to update document: %s in database %s", document.getId(), db.name()), dbexception);
                    throw dbexception;
                }
            }
        }
    }

    private void deleteOutgoingRelations(ArangoDocumentReference document, ArangoConnection connection) {
        if (document != null) {
            ArangoDatabase db = connection.getOrCreateDB();
            ArangoCollection collection = db.collection(document.getCollection().getName());
            if (collection.exists() && collection.getInfo().getType()==CollectionType.DOCUMENT) {
                if (collection.documentExists(document.getKey())) {
                    try {
                        ArangoCursor<String> result = db.query(queryFactory.queryOutboundRelationsForDocument(document, connection.getEdgesCollectionNames(), authorizationContext.getReadableOrganizations()), null, new AqlQueryOptions(), String.class);
                        for (String id : result.asListRemaining()) {
                            deleteDocument(ArangoDocumentReference.fromId(id), db);
                        }
                        logger.info("Deleted document: {} from database {}", document.getId(), db.name());
                    } catch (ArangoDBException dbexception) {
                        logger.error(String.format("Was not able to delete document: %s in database %s", document.getId(), db.name()), dbexception);
                        throw dbexception;
                    }
                } else {
                    logger.debug("Was not able to delete {} because the document does not exist. Skip.", document.getId());
                }
            } else {
                logger.debug("Tried to delete {} although the collection doesn't exist. Skip.", document.getId());
            }
        } else {
            logger.error("Was not able to delete document due to missing id");
        }
    }


    private void deleteDocument(ArangoDocumentReference document, ArangoDatabase db) {
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
                    logger.debug("Was not able to delete {} because the document does not exist. Skip.", document.getId());
                }
            } else {
                logger.debug("Tried to delete {} although the collection doesn't exist. Skip.", document.getId());
            }
        } else {
            logger.error("Was not able to delete document due to missing id");
        }
    }

    public void clearDatabase(ArangoDatabase db) {
        for (CollectionEntity collectionEntity : db.getCollections()) {
            if (!collectionEntity.getName().startsWith("_")) {
                logger.info("Drop collection {} in db {}", collectionEntity.getName(), db.name());
                db.collection(collectionEntity.getName()).drop();
            }
        }
    }

}
