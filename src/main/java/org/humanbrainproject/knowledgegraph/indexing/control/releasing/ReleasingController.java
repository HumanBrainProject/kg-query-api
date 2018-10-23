package org.humanbrainproject.knowledgegraph.indexing.control.releasing;

import org.humanbrainproject.knowledgegraph.indexing.control.ExecutionPlanner;
import org.humanbrainproject.knowledgegraph.indexing.control.IndexingController;
import org.humanbrainproject.knowledgegraph.indexing.control.IndexingProvider;
import org.humanbrainproject.knowledgegraph.indexing.control.MessageProcessor;
import org.humanbrainproject.knowledgegraph.indexing.entity.*;
import org.humanbrainproject.knowledgegraph.indexing.entity.knownSemantics.Release;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusInstanceReference;
import org.humanbrainproject.knowledgegraph.propertyGraph.entity.MainVertex;
import org.humanbrainproject.knowledgegraph.propertyGraph.entity.ResolvedVertexStructure;
import org.humanbrainproject.knowledgegraph.propertyGraph.entity.SubSpace;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ReleasingController implements IndexingController {


    @Autowired
    MessageProcessor messageProcessor;

    @Autowired
    ExecutionPlanner executionPlanner;

    @Autowired
    IndexingProvider indexingProvider;


    @Override
    public void insert(QualifiedIndexingMessage message, TodoList todoList) {
        Release release = new Release(message);
        if (release.isInstance()) {
            NexusInstanceReference instanceToBeReleased = release.getReleaseInstance();
            if(instanceToBeReleased!=null) {
                insert(instanceToBeReleased, todoList);
            }
        }
    }

    private void insert(InstanceReference instanceToBeReleased, TodoList todoList) {
        String payloadFromPrimaryStore = indexingProvider.getPayloadFromPrimaryStore(instanceToBeReleased);
        QualifiedIndexingMessage qualifiedFromPrimaryStore = messageProcessor.qualify(new IndexingMessage(instanceToBeReleased, payloadFromPrimaryStore));
        ResolvedVertexStructure vertexFromPrimaryStore = messageProcessor.createVertexStructure(qualifiedFromPrimaryStore);
        indexingProvider.mapToOriginalSpace(vertexFromPrimaryStore.getMainVertex());
        executionPlanner.addVertexWithEmbeddedInstancesToTodoList(todoList, vertexFromPrimaryStore.getMainVertex(), indexingProvider.getConnection(TargetDatabase.RELEASE), TodoItem.Action.INSERT);
    }

    @Override
    public void update(QualifiedIndexingMessage message, TodoList todoList) {
        Release release =  new Release(message);
        if (release.isInstance()) {
            String payloadById = indexingProvider.getPayloadById(message.getOriginalMessage().getInstanceReference(), TargetDatabase.DEFAULT);
            QualifiedIndexingMessage previousReleaseInstanceFromArango = messageProcessor.qualify(new IndexingMessage(message.getOriginalMessage().getInstanceReference(), payloadById));
            Release previousReleaseFromArango = new Release(previousReleaseInstanceFromArango);
            if (previousReleaseFromArango.isInstance()) {
                if(release.getReleaseInstance()==null && previousReleaseFromArango.getReleaseInstance()!=null){
                    //We can not find the release instance now and we couldn't in the previous release - we skip it!
                    return;
                }
                if(release.getReleaseInstance()!=null){
                    if(!release.getReleaseInstance().equals(previousReleaseFromArango.getReleaseInstance())){
                        if(previousReleaseFromArango.getReleaseInstance()!=null){
                            delete(previousReleaseFromArango.getReleaseInstance(), todoList);
                        }
                        insert(release.getReleaseInstance(), todoList);
                    }

                }
                else{
                    //There was a release before, but now it's not anymore - remove
                    delete(previousReleaseFromArango.getReleaseInstance(), todoList);
                }
            } else {
                //Before, the instance hasn't been a release - so this is the same as an insertion
                insert(message, todoList);
            }
        }
    }


    @Override
    public void delete(InstanceReference instanceToBeRemoved, TodoList todoList) {
        MainVertex originalVertex = indexingProvider.getVertexStructureById(instanceToBeRemoved, TargetDatabase.RELEASE, SubSpace.MAIN);
        executionPlanner.addVertexWithEmbeddedInstancesToTodoList(todoList, originalVertex, indexingProvider.getConnection(TargetDatabase.RELEASE), TodoItem.Action.DELETE);
    }

    @Override
    public void clear() {
        indexingProvider.getConnection(TargetDatabase.RELEASE).clearData();
    }

}
