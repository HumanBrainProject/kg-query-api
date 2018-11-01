package org.humanbrainproject.knowledgegraph.commons.nexusToArangoIndexing.control;

import com.arangodb.ArangoDatabase;
import com.arangodb.entity.CollectionType;
import org.humanbrainproject.knowledgegraph.commons.jsonld.control.JsonTransformer;
import org.humanbrainproject.knowledgegraph.commons.nexus.control.NexusDocumentConverter;
import org.humanbrainproject.knowledgegraph.commons.nexus.control.SystemNexusClient;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.ArangoConnection;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.ArangoDocumentConverter;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.ArangoRepository;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoDocumentReference;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.control.DatabaseTransaction;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.entity.Edge;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.entity.MainVertex;
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
    NexusDocumentConverter nexusDocumentConverter;

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
                ArangoDocumentReference reference = ArangoDocumentReference.fromVertexOrEdgeReference(deleteItem.getReference());
                repository.deleteDocument(reference, database);
            }
        }

        //then add new instances
        List<InsertTodoItem> insertItems = todoList.getInsertTodoItems();
        for (InsertTodoItem insertItem : insertItems) {
            ArangoConnection databaseConnection = insertItem.getDatabaseConnection(ArangoConnection.class);
            if(databaseConnection!=null) {
                ArangoDatabase database = databaseConnection.getOrCreateDB();
                ArangoDocumentReference reference = ArangoDocumentReference.fromVertexOrEdgeReference(insertItem.getObject());
                String jsonFromVertexOrEdge = arangoDocumentConverter.createJsonFromVertexOrEdge(reference, insertItem.getObject());
                if (jsonFromVertexOrEdge != null) {
                    repository.insertDocument(reference, jsonFromVertexOrEdge, insertItem.getObject() instanceof Edge ? CollectionType.EDGES : CollectionType.DOCUMENT, database);
                }
            }
        }

        //and finally trigger primary store insertions/updates.
        List<InsertOrUpdateInPrimaryStoreTodoItem> insertOrUpdateInPrimaryStoreItems = todoList.getInsertOrUpdateInPrimaryStoreTodoItems();
        for (InsertOrUpdateInPrimaryStoreTodoItem insertOrUpdateInPrimaryStoreItem : insertOrUpdateInPrimaryStoreItems) {
            MainVertex mainVertex = insertOrUpdateInPrimaryStoreItem.getObject();
            String jsonFromVertexOrEdge = nexusDocumentConverter.createJsonFromVertex(mainVertex.getInstanceReference(), mainVertex);
            NexusInstanceReference newReference = nexusClient.createOrUpdateInstance(mainVertex.getInstanceReference().setRevision(null), jsonTransformer.parseToMap(jsonFromVertexOrEdge));
            mainVertex.setInstanceReference(newReference);
        }
    }

}
