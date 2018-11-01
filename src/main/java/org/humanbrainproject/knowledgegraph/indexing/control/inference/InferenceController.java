package org.humanbrainproject.knowledgegraph.indexing.control.inference;

import org.humanbrainproject.knowledgegraph.commons.propertyGraph.entity.MainVertex;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.entity.ResolvedVertexStructure;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.entity.SubSpace;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.entity.VertexOrEdgeReference;
import org.humanbrainproject.knowledgegraph.commons.vocabulary.HBPVocabulary;
import org.humanbrainproject.knowledgegraph.indexing.control.ExecutionPlanner;
import org.humanbrainproject.knowledgegraph.indexing.control.IndexingController;
import org.humanbrainproject.knowledgegraph.indexing.control.MessageProcessor;
import org.humanbrainproject.knowledgegraph.indexing.control.nexusToArango.NexusToArangoIndexingProvider;
import org.humanbrainproject.knowledgegraph.indexing.entity.QualifiedIndexingMessage;
import org.humanbrainproject.knowledgegraph.indexing.entity.TargetDatabase;
import org.humanbrainproject.knowledgegraph.indexing.entity.TodoList;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusInstanceReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class InferenceController implements IndexingController{

    private final static List<String> EDGE_BLACKLIST_FOR_INFERENCE = Arrays.asList(HBPVocabulary.INFERENCE_OF, HBPVocabulary.INFERENCE_EXTENDS);

    @Autowired
    MessageProcessor messageProcessor;

    @Autowired
    ExecutionPlanner executionPlanner;

    @Autowired
    NexusToArangoIndexingProvider indexingProvider;

    private Set<InferenceStrategy> strategies = Collections.synchronizedSet(new HashSet<>());


    @Override
    public TodoList insert(QualifiedIndexingMessage message, TodoList todoList){
        if(message.isOfType(HBPVocabulary.INFERENCE_TYPE)){
            ResolvedVertexStructure vertexStructure = messageProcessor.createVertexStructure(message);
            indexingProvider.mapToOriginalSpace(vertexStructure.getMainVertex(), message.getOriginalId());
            executionPlanner.insertVertexWithEmbeddedInstances(todoList, vertexStructure.getMainVertex(), indexingProvider.getConnection(TargetDatabase.INFERRED), EDGE_BLACKLIST_FOR_INFERENCE);
        } else {
            Set<MainVertex> documents = new HashSet<>();
            for (InferenceStrategy strategy : strategies) {
                strategy.infer(message, documents);
            }
            if(documents.isEmpty()){
                ResolvedVertexStructure vertexStructure = messageProcessor.createVertexStructure(message);
                indexingProvider.mapToOriginalSpace(vertexStructure.getMainVertex(), message.getOriginalId());
                executionPlanner.insertVertexWithEmbeddedInstances(todoList, vertexStructure.getMainVertex(), indexingProvider.getConnection(TargetDatabase.INFERRED), EDGE_BLACKLIST_FOR_INFERENCE);
            }
            else{
                documents.forEach(doc -> executionPlanner.insertVertexOrEdgeInPrimaryStore(todoList, doc));
            }
        }
        return todoList;
    }

    @Override
    public TodoList update(QualifiedIndexingMessage message, TodoList todoList) {
        delete(message.getOriginalMessage().getInstanceReference(), todoList, message.getOriginalMessage().getTimestamp(), message.getOriginalMessage().getUserId());
        insert(message, todoList);
        return todoList;
    }

    @Override
    public TodoList delete(NexusInstanceReference reference, TodoList todoList, String timestamp, String userId) {
        NexusInstanceReference originalIdInMainSpace = indexingProvider.findOriginalId(reference).toSubSpace(SubSpace.MAIN);
        Set<VertexOrEdgeReference> vertexOrEdgeReferences = indexingProvider.getVertexOrEdgeReferences(originalIdInMainSpace, TargetDatabase.INFERRED);
        for (VertexOrEdgeReference vertexOrEdgeReference : vertexOrEdgeReferences) {
            executionPlanner.deleteVertexOrEdge(todoList, vertexOrEdgeReference, indexingProvider.getConnection(TargetDatabase.INFERRED));
        }
        if(!reference.getFullId(false).equals(originalIdInMainSpace.getFullId(false))){
            //TODO The delete was not on the original document - so we should trigger the inference by new
        }
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
