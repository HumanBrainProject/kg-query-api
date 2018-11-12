package org.humanbrainproject.knowledgegraph.commons.nexusToArangoIndexing.control;

import com.arangodb.ArangoCollection;
import com.arangodb.ArangoDatabase;
import com.arangodb.entity.CollectionType;
import com.arangodb.model.CollectionCreateOptions;
import org.humanbrainproject.knowledgegraph.commons.jsonld.control.JsonTransformer;
import org.humanbrainproject.knowledgegraph.commons.nexus.control.SystemNexusClient;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.ArangoConnection;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.ArangoDatabaseFactory;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.ArangoDocumentConverter;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.ArangoRepository;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoCollectionReference;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoDocumentReference;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.control.DatabaseTransaction;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.entity.Edge;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.entity.Vertex;
import org.humanbrainproject.knowledgegraph.commons.vocabulary.ArangoVocabulary;
import org.humanbrainproject.knowledgegraph.indexing.entity.DeleteTodoItem;
import org.humanbrainproject.knowledgegraph.indexing.entity.InsertOrUpdateInPrimaryStoreTodoItem;
import org.humanbrainproject.knowledgegraph.indexing.entity.InsertTodoItem;
import org.humanbrainproject.knowledgegraph.indexing.entity.TodoList;
import org.humanbrainproject.knowledgegraph.indexing.entity.knownSemantics.LinkingInstance;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusInstanceReference;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusSchemaReference;
import org.humanbrainproject.knowledgegraph.instances.control.InstanceController;
import org.humanbrainproject.knowledgegraph.query.entity.JsonDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;

@Component
@Primary
public class NexusArangoTransaction implements DatabaseTransaction {

    @Autowired
    ArangoRepository repository;

    @Autowired
    ArangoDocumentConverter arangoDocumentConverter;

    @Autowired
    SystemNexusClient nexusClient;

    @Autowired
    JsonTransformer jsonTransformer;

    @Autowired
    InstanceController instanceController;

    @Autowired
    ArangoDatabaseFactory databaseFactory;

    protected Logger logger = LoggerFactory.getLogger(NexusArangoTransaction.class);


    void createTypeLookup(NexusSchemaReference nexusSchemaReference){
        ArangoDatabase internalDB = databaseFactory.getInternalDB().getOrCreateDB();
        ArangoCollection collection = internalDB.collection(ArangoVocabulary.LOOKUP_COLLECTION);
        if(!collection.exists()){
            internalDB.createCollection(ArangoVocabulary.LOOKUP_COLLECTION);
           collection = internalDB.collection(ArangoVocabulary.LOOKUP_COLLECTION);
        }
        ArangoCollectionReference collectionReference = ArangoCollectionReference.fromNexusSchemaReference(nexusSchemaReference);
        JsonDocument jsonDocument = new JsonDocument();
        jsonDocument.put(ArangoVocabulary.LOOKUP_SCHEMAS, nexusSchemaReference.getRelativeUrl().getUrl());
        jsonDocument.put(ArangoVocabulary.KEY, collectionReference.getName());
        if(collection.documentExists(collectionReference.getName())){
            collection.updateDocument(collectionReference.getName(), jsonDocument);
        }
        else{
            collection.insertDocument(jsonDocument);
        }

    }

    @Override
    public void execute(TodoList todoList) {
        //First remove instances
        List<DeleteTodoItem> deleteItems = todoList.getDeleteTodoItems();
        for (DeleteTodoItem deleteItem : deleteItems) {
            ArangoConnection databaseConnection = deleteItem.getDatabaseConnection(ArangoConnection.class);
            if(databaseConnection!=null) {
                ArangoDatabase database = databaseConnection.getOrCreateDB();
                ArangoDocumentReference reference = ArangoDocumentReference.fromNexusInstance(deleteItem.getReference());
                repository.deleteOutgoingRelations(reference, databaseConnection);
                repository.deleteDocument(reference, database);
            }
        }

        //then add new instances
        List<InsertTodoItem> insertItems = todoList.getInsertTodoItems();
        for (InsertTodoItem insertItem : insertItems) {
            createTypeLookup(insertItem.getVertex().getInstanceReference().getNexusSchema());
            ArangoConnection databaseConnection = insertItem.getDatabaseConnection(ArangoConnection.class);
            if(databaseConnection!=null) {
                ArangoDatabase database = databaseConnection.getOrCreateDB();
                Vertex vertex = insertItem.getVertex();
                ArangoDocumentReference reference = ArangoDocumentReference.fromNexusInstance(vertex.getInstanceReference());
                LinkingInstance linkingInstance = new LinkingInstance(vertex.getQualifiedIndexingMessage());
                if(linkingInstance.isInstance()) {
                    ArangoDocumentReference documentReference = ArangoDocumentReference.fromNexusInstance(vertex.getInstanceReference());
                    repository.deleteDocument(documentReference, database);
                    if(linkingInstance.getFrom()!=null && linkingInstance.getTo()!=null) {
                        String jsonFromLinkingInstance = arangoDocumentConverter.createJsonFromLinkingInstance(documentReference, linkingInstance.getFrom(), linkingInstance.getTo(), vertex.getInstanceReference());
                        repository.insertDocument(documentReference, jsonFromLinkingInstance, CollectionType.EDGES, database);
                    }
                }
                else {
                    //Remove already existing instances
                    repository.deleteOutgoingRelations(reference, databaseConnection);
                    repository.deleteDocument(reference, database);

                    String vertexJson = arangoDocumentConverter.createJsonFromVertex(reference, vertex, insertItem.getBlacklist());
                    if (vertexJson != null) {
                        repository.insertDocument(reference, vertexJson, CollectionType.DOCUMENT, database);
                    }
                    for (Edge edge : vertex.getEdges()) {
                        ArangoDocumentReference document = ArangoDocumentReference.fromEdge(edge);
                        String jsonFromEdge = arangoDocumentConverter.createJsonFromEdge(document, vertex, edge, insertItem.getBlacklist());
                        repository.insertDocument(document, jsonFromEdge, CollectionType.EDGES, database);
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
            NexusInstanceReference newReference = nexusClient.createOrUpdateInstance(vertex.getInstanceReference().setRevision(null), new LinkedHashMap<>(vertex.getQualifiedIndexingMessage().getQualifiedMap()));
            vertex.setInstanceReference(newReference);
        }
    }

}
