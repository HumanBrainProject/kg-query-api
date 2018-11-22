package org.humanbrainproject.knowledgegraph.indexing.control.inference;

import org.humanbrainproject.knowledgegraph.commons.authorization.entity.OidcAccessToken;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.entity.JsonPath;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.entity.SubSpace;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.entity.Vertex;
import org.humanbrainproject.knowledgegraph.commons.vocabulary.HBPVocabulary;
import org.humanbrainproject.knowledgegraph.indexing.control.IndexingController;
import org.humanbrainproject.knowledgegraph.indexing.control.MessageProcessor;
import org.humanbrainproject.knowledgegraph.indexing.control.nexusToArango.NexusToArangoIndexingProvider;
import org.humanbrainproject.knowledgegraph.indexing.entity.*;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusInstanceReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class InferenceController implements IndexingController{

    private final static List<JsonPath> EDGE_BLACKLIST_FOR_INFERENCE = Arrays.asList(new JsonPath(HBPVocabulary.INFERENCE_OF), new JsonPath(HBPVocabulary.INFERENCE_EXTENDS));

    @Autowired
    MessageProcessor messageProcessor;

    @Autowired
    NexusToArangoIndexingProvider indexingProvider;

    private Set<InferenceStrategy> strategies = Collections.synchronizedSet(new HashSet<>());

    @Override
    public TodoList insert(QualifiedIndexingMessage message, TodoList todoList, OidcAccessToken oidcAccessToken){
        if(message.isOfType(HBPVocabulary.INFERENCE_TYPE)){
            insertVertexStructure(message, todoList, oidcAccessToken);
        } else {
            Set<Vertex> documents = new HashSet<>();
            for (InferenceStrategy strategy : strategies) {
                strategy.infer(message, documents, oidcAccessToken);
            }
            if(documents.isEmpty()){
                insertVertexStructure(message, todoList, oidcAccessToken);
            }
            else{
                documents.forEach(doc -> {
                    todoList.addTodoItem(new InsertOrUpdateInPrimaryStoreTodoItem(doc));
                });
            }
        }
        return todoList;
    }

    private void insertVertexStructure(QualifiedIndexingMessage message, TodoList todoList, OidcAccessToken oidcAccessToken) {
        Vertex vertexStructure = messageProcessor.createVertexStructure(message);
        vertexStructure = indexingProvider.mapToOriginalSpace(vertexStructure, message.getOriginalId(), oidcAccessToken);
        InsertTodoItem insertTodoItem = new InsertTodoItem(vertexStructure, indexingProvider.getConnection(TargetDatabase.INFERRED));
        insertTodoItem.getBlacklist().addAll(EDGE_BLACKLIST_FOR_INFERENCE);
        todoList.addTodoItem(insertTodoItem);
    }

    @Override
    public TodoList update(QualifiedIndexingMessage message, TodoList todoList, OidcAccessToken oidcAccessToken) {
        //delete(message.getOriginalMessage().getInstanceReference(), todoList);
        insert(message, todoList, oidcAccessToken);
        return todoList;
    }

    @Override
    public TodoList delete(NexusInstanceReference reference, TodoList todoList, OidcAccessToken oidcAccessToken) {
        NexusInstanceReference originalIdInMainSpace = indexingProvider.findOriginalId(reference, oidcAccessToken).toSubSpace(SubSpace.MAIN);
        todoList.addTodoItem(new DeleteTodoItem(originalIdInMainSpace, indexingProvider.getConnection(TargetDatabase.INFERRED)));
        return todoList;
    }

    @Override
    public void clear(OidcAccessToken oidcAccessToken) {
        indexingProvider.getConnection(TargetDatabase.INFERRED).clearData();
    }

    void addInferenceStrategy(InferenceStrategy strategy) {
        strategies.add(strategy);
    }

}
