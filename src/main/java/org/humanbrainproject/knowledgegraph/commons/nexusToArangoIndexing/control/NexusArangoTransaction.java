package org.humanbrainproject.knowledgegraph.commons.nexusToArangoIndexing.control;

import com.arangodb.ArangoDatabase;
import com.arangodb.entity.CollectionType;
import org.humanbrainproject.knowledgegraph.commons.jsonld.control.JsonTransformer;
import org.humanbrainproject.knowledgegraph.commons.nexus.control.NexusDocumentConverter;
import org.humanbrainproject.knowledgegraph.commons.nexus.control.SystemNexusClient;
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
public class NexusArangoTransaction implements DatabaseTransaction<ArangoDatabase> {

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
    public void execute(TodoList<ArangoDatabase> todoList) {
        //First remove instances
        List<DeleteTodoItem<ArangoDatabase>> deleteItems = todoList.getDeleteTodoItems();
        for (DeleteTodoItem<ArangoDatabase> deleteItem : deleteItems) {
            ArangoDatabase database = deleteItem.getDatabaseConnection().getOrCreateDB();
            ArangoDocumentReference reference = ArangoDocumentReference.fromVertexOrEdgeReference(deleteItem.getReference());
            repository.deleteDocument(reference, database);
        }

        //then add new instances
        List<InsertTodoItem<ArangoDatabase>> insertItems = todoList.getInsertTodoItems();
        for (InsertTodoItem<ArangoDatabase> insertItem : insertItems) {
            ArangoDatabase database = insertItem.getDatabaseConnection().getOrCreateDB();
            ArangoDocumentReference reference = ArangoDocumentReference.fromVertexOrEdgeReference(insertItem.getObject());
            String jsonFromVertexOrEdge = arangoDocumentConverter.createJsonFromVertexOrEdge(reference, insertItem.getObject());
            if (jsonFromVertexOrEdge != null) {
                repository.insertDocument(reference, jsonFromVertexOrEdge, insertItem.getObject() instanceof Edge ? CollectionType.EDGES : CollectionType.DOCUMENT, database);
            }
        }

        //and finally trigger primary store insertions/updates.
        List<InsertOrUpdateInPrimaryStoreTodoItem<ArangoDatabase>> insertOrUpdateInPrimaryStoreItems = todoList.getInsertOrUpdateInPrimaryStoreTodoItems();
        for (InsertOrUpdateInPrimaryStoreTodoItem<ArangoDatabase> insertOrUpdateInPrimaryStoreItem : insertOrUpdateInPrimaryStoreItems) {
            MainVertex mainVertex = insertOrUpdateInPrimaryStoreItem.getObject();
            String jsonFromVertexOrEdge = nexusDocumentConverter.createJsonFromVertex(mainVertex.getInstanceReference(), mainVertex);
            NexusInstanceReference newReference = nexusClient.createOrUpdateInstance(mainVertex.getInstanceReference().setRevision(null), jsonTransformer.parseToMap(jsonFromVertexOrEdge));
            mainVertex.setInstanceReference(newReference);
        }
    }

}
