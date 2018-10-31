package org.humanbrainproject.knowledgegraph.indexing.control.releasing;

import org.humanbrainproject.knowledgegraph.commons.propertyGraph.entity.ResolvedVertexStructure;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.entity.SubSpace;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.entity.VertexOrEdgeReference;
import org.humanbrainproject.knowledgegraph.indexing.control.ExecutionPlanner;
import org.humanbrainproject.knowledgegraph.indexing.control.IndexingController;
import org.humanbrainproject.knowledgegraph.indexing.control.IndexingProvider;
import org.humanbrainproject.knowledgegraph.indexing.control.MessageProcessor;
import org.humanbrainproject.knowledgegraph.indexing.entity.IndexingMessage;
import org.humanbrainproject.knowledgegraph.indexing.entity.QualifiedIndexingMessage;
import org.humanbrainproject.knowledgegraph.indexing.entity.TargetDatabase;
import org.humanbrainproject.knowledgegraph.indexing.entity.TodoList;
import org.humanbrainproject.knowledgegraph.indexing.entity.knownSemantics.Release;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusInstanceReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class ReleasingController implements IndexingController {


    @Autowired
    MessageProcessor messageProcessor;

    @Autowired
    ExecutionPlanner executionPlanner;

    @Autowired
    IndexingProvider indexingProvider;

    @Override
    public <T> TodoList<T> insert(QualifiedIndexingMessage message, TodoList<T> todoList) {
        Release release = new Release(message);
        if (release.isInstance()) {
            NexusInstanceReference instanceToBeReleased = release.getReleaseInstance();
            if(instanceToBeReleased!=null) {
                insert(instanceToBeReleased, todoList);
            }
        }
        return todoList;
    }

    private <T> void insert(NexusInstanceReference instanceToBeReleased, TodoList<T> todoList) {
        String payloadFromPrimaryStore = indexingProvider.getPayloadFromPrimaryStore(instanceToBeReleased);
        QualifiedIndexingMessage qualifiedFromPrimaryStore = messageProcessor.qualify(new IndexingMessage(instanceToBeReleased, payloadFromPrimaryStore));
        ResolvedVertexStructure vertexFromPrimaryStore = messageProcessor.createVertexStructure(qualifiedFromPrimaryStore);
        indexingProvider.mapToOriginalSpace(vertexFromPrimaryStore.getMainVertex(), vertexFromPrimaryStore.getQualifiedMessage().getOriginalMessage().getInstanceReference());
        executionPlanner.insertVertexWithEmbeddedInstances(todoList, vertexFromPrimaryStore.getMainVertex(), indexingProvider.getConnection(TargetDatabase.RELEASE), null);
    }

    @Override
    public <T> TodoList<T> update(QualifiedIndexingMessage message, TodoList<T> todoList) {
        Release release =  new Release(message);
        if (release.isInstance()) {
            deleteRelease(message.getOriginalMessage().getInstanceReference(), todoList);
            insert(message, todoList);
        }
        return todoList;
    }

    public <T> TodoList<T> deleteRelease(NexusInstanceReference releaseInstance, TodoList<T> todoList) {
        String payloadById = indexingProvider.getPayloadById(releaseInstance, TargetDatabase.DEFAULT);
        if(payloadById!=null) {
            QualifiedIndexingMessage previousReleaseInstanceFromArango = messageProcessor.qualify(new IndexingMessage(releaseInstance, payloadById));
            Release previousReleaseFromArango = new Release(previousReleaseInstanceFromArango);
            if(previousReleaseFromArango.isInstance() && previousReleaseFromArango.getReleaseInstance()!=null){
                //We only have to remove stuff if the releaseInstance is a release
                NexusInstanceReference originalIdInMainSpace = indexingProvider.findOriginalId(previousReleaseFromArango.getReleaseInstance()).toSubSpace(SubSpace.MAIN);
                //Since the releasing normalizes the ids to the mainspace, we have to resolve the identifier first. Finally, we remove everything which belongs to this group.
                Set<VertexOrEdgeReference> vertexOrEdgeReferences = indexingProvider.getVertexOrEdgeReferences(originalIdInMainSpace, TargetDatabase.RELEASE);
                for (VertexOrEdgeReference vertexOrEdgeReference : vertexOrEdgeReferences) {
                    executionPlanner.deleteVertexOrEdge(todoList, vertexOrEdgeReference, indexingProvider.getConnection(TargetDatabase.RELEASE));
                }
            }
        }
        return todoList;
    }


    @Override
    public <T> TodoList<T> delete(NexusInstanceReference instanceToBeRemoved, TodoList<T> todoList) {
        //If the instance to be removed is a release-instance, we take care that all related instances are removed (unrelease)
        deleteRelease(instanceToBeRemoved, todoList);

        //Otherwise, regardless of which element of the instance group is deleted, we remove all of them from the released space.
        NexusInstanceReference originalIdInMainSpace = indexingProvider.findOriginalId(instanceToBeRemoved).toSubSpace(SubSpace.MAIN);
        Set<VertexOrEdgeReference> vertexOrEdgeReferences = indexingProvider.getVertexOrEdgeReferences(originalIdInMainSpace, TargetDatabase.RELEASE);
        for (VertexOrEdgeReference vertexOrEdgeReference : vertexOrEdgeReferences) {
            executionPlanner.deleteVertexOrEdge(todoList, vertexOrEdgeReference, indexingProvider.getConnection(TargetDatabase.RELEASE));
        }
        return todoList;
    }

    @Override
    public void clear() {
        indexingProvider.getConnection(TargetDatabase.RELEASE).clearData();
    }

}
