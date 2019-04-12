package org.humanbrainproject.knowledgegraph.indexing.control.releasing;

import org.humanbrainproject.knowledgegraph.annotations.ToBeTested;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.ArangoNativeRepository;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.entity.SubSpace;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.entity.Vertex;
import org.humanbrainproject.knowledgegraph.commons.vocabulary.HBPVocabulary;
import org.humanbrainproject.knowledgegraph.context.QueryContext;
import org.humanbrainproject.knowledgegraph.indexing.control.IndexingController;
import org.humanbrainproject.knowledgegraph.indexing.control.MessageProcessor;
import org.humanbrainproject.knowledgegraph.indexing.control.nexusToArango.NexusToArangoIndexingProvider;
import org.humanbrainproject.knowledgegraph.indexing.entity.*;
import org.humanbrainproject.knowledgegraph.indexing.entity.knownSemantics.Release;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusInstanceReference;
import org.humanbrainproject.knowledgegraph.indexing.entity.todo.DeleteTodoItem;
import org.humanbrainproject.knowledgegraph.indexing.entity.todo.InsertTodoItem;
import org.humanbrainproject.knowledgegraph.indexing.entity.todo.TodoList;
import org.humanbrainproject.knowledgegraph.instances.control.InstanceLookupController;
import org.humanbrainproject.knowledgegraph.query.entity.DatabaseScope;
import org.humanbrainproject.knowledgegraph.query.entity.JsonDocument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;

/**
 * The releasing controller takes care of the proper management of the released instances.
 * It checks if the incoming message has the semantic of a release flag - if so, it takes the corresponding actions to
 * figure out which instances shall be released / re-released / unreleased.
 */
@ToBeTested
@Component
public class ReleasingController implements IndexingController {


    @Autowired
    MessageProcessor messageProcessor;

    @Autowired
    NexusToArangoIndexingProvider indexingProvider;

    @Autowired
    QueryContext queryContext;

    @Autowired
    InstanceLookupController instances;

    @Autowired
    ArangoNativeRepository arangoNativeRepository;

    @Override
    public TodoList insert(QualifiedIndexingMessage message, TodoList todoList) {
        Release release = new Release(message);
        if (release.isInstance()) {
            NexusInstanceReference instanceToBeReleased = release.getReleaseInstance();
            if (instanceToBeReleased != null) {
                insert(instanceToBeReleased, todoList, message.getOriginalMessage().getTimestamp(), message.getOriginalMessage().getUserId());
            }
        }
        return todoList;
    }

    private void insert(NexusInstanceReference instanceToBeReleased, TodoList todoList, String timestamp, String userId) {
        String payloadFromPrimaryStore = indexingProvider.getPayloadFromPrimaryStore(instanceToBeReleased);
        QualifiedIndexingMessage qualifiedFromPrimaryStore = messageProcessor.qualify(new IndexingMessage(instanceToBeReleased, payloadFromPrimaryStore, timestamp, userId));
        NexusInstanceReference originalId = arangoNativeRepository.findOriginalId(instanceToBeReleased);
        queryContext.setDatabaseScope(DatabaseScope.RELEASED);
        JsonDocument currentlyReleased = this.instances.getInstance(originalId);
        long timeInMS =  ZonedDateTime.parse(timestamp).toEpochSecond() * 1000;
        //First release
        if(currentlyReleased == null){
            qualifiedFromPrimaryStore.getQualifiedMap().put(HBPVocabulary.RELEASE_FIRST_DATE, timeInMS);
            qualifiedFromPrimaryStore.getQualifiedMap().put(HBPVocabulary.RELEASE_FIRST_BY, userId);
        } else{
            long firstRel;
            if(currentlyReleased.get( HBPVocabulary.RELEASE_FIRST_DATE) instanceof Long){
                firstRel = (Long) currentlyReleased.get( HBPVocabulary.RELEASE_FIRST_DATE);
            }else{
                firstRel =  ZonedDateTime.parse((String) currentlyReleased.get( HBPVocabulary.RELEASE_FIRST_DATE)).toEpochSecond() * 1000;

            }
            String firstRelBy = (String) currentlyReleased.get( HBPVocabulary.RELEASE_FIRST_BY);
            qualifiedFromPrimaryStore.getQualifiedMap().put(HBPVocabulary.RELEASE_FIRST_DATE, firstRel);
            qualifiedFromPrimaryStore.getQualifiedMap().put(HBPVocabulary.RELEASE_FIRST_BY, firstRelBy);
        }
        qualifiedFromPrimaryStore.getQualifiedMap().put(HBPVocabulary.RELEASE_LAST_DATE, timeInMS);
        qualifiedFromPrimaryStore.getQualifiedMap().put(HBPVocabulary.RELEASE_LAST_BY, userId);

        Vertex vertexFromPrimaryStore = messageProcessor.createVertexStructure(qualifiedFromPrimaryStore);
        vertexFromPrimaryStore = indexingProvider.mapToOriginalSpace(vertexFromPrimaryStore, vertexFromPrimaryStore.getQualifiedIndexingMessage().getOriginalId());
        todoList.addTodoItem(new InsertTodoItem(vertexFromPrimaryStore, indexingProvider.getConnection(TargetDatabase.RELEASE)));
    }

    @Override
    public TodoList update(QualifiedIndexingMessage message, TodoList todoList) {
        Release release = new Release(message);
        if (release.isInstance()) {
            deleteRelease(message.getOriginalMessage().getInstanceReference(), todoList);
            insert(message, todoList);
        }
        return todoList;
    }

    public void deleteRelease(NexusInstanceReference releaseInstance, TodoList todoList) {
        String payloadById = indexingProvider.getPayloadById(releaseInstance, TargetDatabase.NATIVE);
        if (payloadById != null) {
            QualifiedIndexingMessage previousReleaseInstanceFromArango = messageProcessor.qualify(new IndexingMessage(releaseInstance, payloadById, null, null));
            Release previousReleaseFromArango = new Release(previousReleaseInstanceFromArango);
            if (previousReleaseFromArango.isInstance() && previousReleaseFromArango.getReleaseInstance() != null) {
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
