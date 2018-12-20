package org.humanbrainproject.knowledgegraph.indexing.control.inference;

import org.humanbrainproject.knowledgegraph.commons.authorization.entity.Credential;
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
    public TodoList insert(QualifiedIndexingMessage message, TodoList todoList, Credential credential){
        if(message.isOfType(HBPVocabulary.INFERENCE_TYPE) || message.isOfType(HBPVocabulary.LINKING_INSTANCE_TYPE)){
            insertVertexStructure(message, todoList, credential);
        } else {
            Set<Vertex> documents = new HashSet<>();
            for (InferenceStrategy strategy : strategies) {
                strategy.infer(message, documents, credential);
            }
            if(documents.isEmpty()){
                insertVertexStructure(message, todoList, credential);
            }
            else{
                documents.forEach(doc -> {
                    todoList.addTodoItem(new InsertOrUpdateInPrimaryStoreTodoItem(doc));
                });
            }
        }
        return todoList;
    }

    private void insertVertexStructure(QualifiedIndexingMessage message, TodoList todoList, Credential credential) {
        Vertex vertexStructure = messageProcessor.createVertexStructure(message);
        vertexStructure = indexingProvider.mapToOriginalSpace(vertexStructure, message.getOriginalId(), credential);
        InsertTodoItem insertTodoItem = new InsertTodoItem(vertexStructure, indexingProvider.getConnection(TargetDatabase.INFERRED));
        insertTodoItem.getBlacklist().addAll(EDGE_BLACKLIST_FOR_INFERENCE);
        todoList.addTodoItem(insertTodoItem);
    }

    @Override
    public TodoList update(QualifiedIndexingMessage message, TodoList todoList, Credential credential) {
        //delete(message.getOriginalMessage().getInstanceReference(), todoList);
        insert(message, todoList, credential);
        return todoList;
    }

    @Override
    public TodoList delete(NexusInstanceReference reference, TodoList todoList, Credential credential) {
        NexusInstanceReference originalIdInMainSpace = indexingProvider.findOriginalId(reference, credential).toSubSpace(SubSpace.MAIN);
        todoList.addTodoItem(new DeleteTodoItem(originalIdInMainSpace, indexingProvider.getConnection(TargetDatabase.INFERRED)));
        return todoList;
    }

    @Override
    public void clear(Credential credential) {
        indexingProvider.getConnection(TargetDatabase.INFERRED).clearData();
    }

    void addInferenceStrategy(InferenceStrategy strategy) {
        strategies.add(strategy);
    }

}
