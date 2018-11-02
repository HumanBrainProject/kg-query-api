package org.humanbrainproject.knowledgegraph.indexing.control.inference;

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
    public TodoList insert(QualifiedIndexingMessage message, TodoList todoList){
        if(message.isOfType(HBPVocabulary.INFERENCE_TYPE)){
            insertVertexStructure(message, todoList);
        } else {
            Set<Vertex> documents = new HashSet<>();
            for (InferenceStrategy strategy : strategies) {
                strategy.infer(message, documents);
            }
            if(documents.isEmpty()){
                insertVertexStructure(message, todoList);
            }
            else{
                documents.forEach(doc -> {
                    todoList.addTodoItem(new InsertOrUpdateInPrimaryStoreTodoItem(doc));
                });
            }
        }
        return todoList;
    }

    private void insertVertexStructure(QualifiedIndexingMessage message, TodoList todoList) {
        Vertex vertexStructure = messageProcessor.createVertexStructure(message);
        indexingProvider.mapToOriginalSpace(vertexStructure, message.getOriginalId());
        InsertTodoItem insertTodoItem = new InsertTodoItem(vertexStructure, indexingProvider.getConnection(TargetDatabase.INFERRED));
        insertTodoItem.getBlacklist().addAll(EDGE_BLACKLIST_FOR_INFERENCE);
        todoList.addTodoItem(insertTodoItem);
    }

    @Override
    public TodoList update(QualifiedIndexingMessage message, TodoList todoList) {
        //delete(message.getOriginalMessage().getInstanceReference(), todoList);
        insert(message, todoList);
        return todoList;
    }

    @Override
    public TodoList delete(NexusInstanceReference reference, TodoList todoList) {
        NexusInstanceReference originalIdInMainSpace = indexingProvider.findOriginalId(reference).toSubSpace(SubSpace.MAIN);
        todoList.addTodoItem(new DeleteTodoItem(originalIdInMainSpace, indexingProvider.getConnection(TargetDatabase.INFERRED)));
        return todoList;
    }

    @Override
    public void clear() {
        indexingProvider.getConnection(TargetDatabase.INFERRED).clearData();
    }

    void addInferenceStrategy(InferenceStrategy strategy) {
        strategies.add(strategy);
    }

}
