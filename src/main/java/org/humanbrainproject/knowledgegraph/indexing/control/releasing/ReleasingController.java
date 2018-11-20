package org.humanbrainproject.knowledgegraph.indexing.control.releasing;

import org.humanbrainproject.knowledgegraph.commons.propertyGraph.entity.SubSpace;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.entity.Vertex;
import org.humanbrainproject.knowledgegraph.indexing.control.IndexingController;
import org.humanbrainproject.knowledgegraph.indexing.control.MessageProcessor;
import org.humanbrainproject.knowledgegraph.indexing.control.nexusToArango.NexusToArangoIndexingProvider;
import org.humanbrainproject.knowledgegraph.indexing.entity.*;
import org.humanbrainproject.knowledgegraph.indexing.entity.knownSemantics.Release;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusInstanceReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ReleasingController implements IndexingController {


    @Autowired
    MessageProcessor messageProcessor;

    @Autowired
    NexusToArangoIndexingProvider indexingProvider;

    @Override
    public TodoList insert(QualifiedIndexingMessage message, TodoList todoList) {
        Release release = new Release(message);
        if (release.isInstance()) {
            NexusInstanceReference instanceToBeReleased = release.getReleaseInstance();
            if(instanceToBeReleased!=null) {
                insert(instanceToBeReleased, todoList, message.getOriginalMessage().getTimestamp(), message.getOriginalMessage().getUserId());
            }
        }
        return todoList;
    }

    private void insert(NexusInstanceReference instanceToBeReleased, TodoList todoList, String timestamp, String userId) {
        String payloadFromPrimaryStore = indexingProvider.getPayloadFromPrimaryStore(instanceToBeReleased);
        QualifiedIndexingMessage qualifiedFromPrimaryStore = messageProcessor.qualify(new IndexingMessage(instanceToBeReleased, payloadFromPrimaryStore, timestamp, userId));
        Vertex vertexFromPrimaryStore = messageProcessor.createVertexStructure(qualifiedFromPrimaryStore);
        vertexFromPrimaryStore = indexingProvider.mapToOriginalSpace(vertexFromPrimaryStore, vertexFromPrimaryStore.getQualifiedIndexingMessage().getOriginalId());
        todoList.addTodoItem(new InsertTodoItem(vertexFromPrimaryStore, indexingProvider.getConnection(TargetDatabase.RELEASE)));
    }

    @Override
    public TodoList update(QualifiedIndexingMessage message, TodoList todoList) {
        Release release =  new Release(message);
        if (release.isInstance()) {
            deleteRelease(message.getOriginalMessage().getInstanceReference(), todoList);
            insert(message, todoList);
        }
        return todoList;
    }

    public void deleteRelease(NexusInstanceReference releaseInstance, TodoList todoList) {
        String payloadById = indexingProvider.getPayloadById(releaseInstance, TargetDatabase.DEFAULT);
        if(payloadById!=null) {
            QualifiedIndexingMessage previousReleaseInstanceFromArango = messageProcessor.qualify(new IndexingMessage(releaseInstance, payloadById, null, null));
            Release previousReleaseFromArango = new Release(previousReleaseInstanceFromArango);
            if(previousReleaseFromArango.isInstance() && previousReleaseFromArango.getReleaseInstance()!=null){
                //We only have to remove stuff if the releaseInstance is a release
                NexusInstanceReference originalIdInMainSpace = indexingProvider.findOriginalId(previousReleaseFromArango.getReleaseInstance()).toSubSpace(SubSpace.MAIN);
                todoList.addTodoItem(new DeleteTodoItem(originalIdInMainSpace, indexingProvider.getConnection(TargetDatabase.RELEASE)));
            }
        }
    }


    @Override
    public TodoList delete(NexusInstanceReference instanceToBeRemoved, TodoList todoList) {
        //If the instance to be removed is a release-instance, we take care that all related instances are removed (unrelease)
        deleteRelease(instanceToBeRemoved, todoList);

        //Otherwise, regardless of which element of the instance group is deleted, we remove all of them from the released space.
        NexusInstanceReference originalIdInMainSpace = indexingProvider.findOriginalId(instanceToBeRemoved).toSubSpace(SubSpace.MAIN);
        todoList.addTodoItem(new DeleteTodoItem(originalIdInMainSpace, indexingProvider.getConnection(TargetDatabase.RELEASE)));
        return todoList;
    }

    @Override
    public void clear() {
        indexingProvider.getConnection(TargetDatabase.RELEASE).clearData();
    }

}
