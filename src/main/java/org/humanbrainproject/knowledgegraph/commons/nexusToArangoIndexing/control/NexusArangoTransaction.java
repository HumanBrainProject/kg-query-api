package org.humanbrainproject.knowledgegraph.commons.nexusToArangoIndexing.control;

import com.arangodb.ArangoDatabase;
import com.arangodb.entity.CollectionType;
import org.humanbrainproject.knowledgegraph.commons.jsonld.control.JsonTransformer;
import org.humanbrainproject.knowledgegraph.commons.nexus.control.SystemNexusClient;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.ArangoConnection;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.ArangoDocumentConverter;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.ArangoRepository;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoDocumentReference;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.control.DatabaseTransaction;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.entity.EdgeX;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.entity.Vertex;
import org.humanbrainproject.knowledgegraph.indexing.entity.DeleteTodoItem;
import org.humanbrainproject.knowledgegraph.indexing.entity.InsertOrUpdateInPrimaryStoreTodoItem;
import org.humanbrainproject.knowledgegraph.indexing.entity.InsertTodoItem;
import org.humanbrainproject.knowledgegraph.indexing.entity.TodoList;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusInstanceReference;
import org.humanbrainproject.knowledgegraph.nexusExt.control.InstanceController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

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

    protected Logger logger = LoggerFactory.getLogger(NexusArangoTransaction.class);

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
            ArangoConnection databaseConnection = insertItem.getDatabaseConnection(ArangoConnection.class);
            if(databaseConnection!=null) {
                ArangoDatabase database = databaseConnection.getOrCreateDB();
                Vertex vertex = insertItem.getVertex();
                ArangoDocumentReference reference = ArangoDocumentReference.fromNexusInstance(vertex.getInstanceReference());

                //Remove already existing instances
                repository.deleteOutgoingRelations(reference, databaseConnection);
                repository.deleteDocument(reference, database);

                String vertexJson = arangoDocumentConverter.createJsonFromVertex(reference, vertex, insertItem.getBlacklist());
                if (vertexJson != null) {
                    repository.insertDocument(reference, vertexJson, CollectionType.DOCUMENT, database);
                }
                for (EdgeX edge : vertex.getEdges()) {
                    ArangoDocumentReference document = ArangoDocumentReference.fromEdge(edge);
                    String jsonFromEdge = arangoDocumentConverter.createJsonFromEdge(document, vertex, edge, insertItem.getBlacklist());
                    repository.insertDocument(document, jsonFromEdge, CollectionType.EDGES, database);
                }
            }
        }

        //and finally trigger primary store insertions/updates.
        List<InsertOrUpdateInPrimaryStoreTodoItem> insertOrUpdateInPrimaryStoreItems = todoList.getInsertOrUpdateInPrimaryStoreTodoItems();
        for (InsertOrUpdateInPrimaryStoreTodoItem insertOrUpdateInPrimaryStoreItem : insertOrUpdateInPrimaryStoreItems) {
            Vertex vertex = insertOrUpdateInPrimaryStoreItem.getVertex();
            NexusInstanceReference newReference = nexusClient.createOrUpdateInstance(vertex.getInstanceReference().setRevision(null), vertex.getQualifiedIndexingMessage().getQualifiedMap());
            vertex.setInstanceReference(newReference);
        }
    }

}
