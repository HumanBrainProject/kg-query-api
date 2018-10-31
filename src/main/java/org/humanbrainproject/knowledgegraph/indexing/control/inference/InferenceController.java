package org.humanbrainproject.knowledgegraph.indexing.control.inference;

import org.humanbrainproject.knowledgegraph.commons.propertyGraph.entity.MainVertex;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.entity.ResolvedVertexStructure;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.entity.SubSpace;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.entity.VertexOrEdgeReference;
import org.humanbrainproject.knowledgegraph.commons.vocabulary.HBPVocabulary;
import org.humanbrainproject.knowledgegraph.indexing.control.ExecutionPlanner;
import org.humanbrainproject.knowledgegraph.indexing.control.IndexingController;
import org.humanbrainproject.knowledgegraph.indexing.control.IndexingProvider;
import org.humanbrainproject.knowledgegraph.indexing.control.MessageProcessor;
import org.humanbrainproject.knowledgegraph.indexing.entity.QualifiedIndexingMessage;
import org.humanbrainproject.knowledgegraph.indexing.entity.TargetDatabase;
import org.humanbrainproject.knowledgegraph.indexing.entity.TodoList;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusInstanceReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class InferenceController implements IndexingController{

    public final static String INFERRED_BASE = HBPVocabulary.NAMESPACE + "inference/";
    public final static String INFERRED_TYPE = INFERRED_BASE + "Inferred";
    public final static String INFERRED_SOURCE = INFERRED_BASE + "source";

    //TODO normalize namespace

    public final static String INFERENCE_OF_PROPERTY = HBPVocabulary.NAMESPACE+"inferenceOf";
    public final static String ORIGINAL_PARENT_PROPERTY = "http://hbp.eu/reconciled#original_parent";

    private final static List<String> EDGE_BLACKLIST_FOR_INFERENCE = Arrays.asList(INFERENCE_OF_PROPERTY, ORIGINAL_PARENT_PROPERTY);

    @Autowired
    MessageProcessor messageProcessor;

    @Autowired
    ExecutionPlanner executionPlanner;

    @Autowired
    IndexingProvider indexingProvider;

    private Set<InferenceStrategy> strategies = Collections.synchronizedSet(new HashSet<>());


    @Override
    public <T> TodoList<T> insert(QualifiedIndexingMessage message, TodoList<T> todoList){
        if(message.isOfType(INFERRED_TYPE)){
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
    public <T> TodoList<T> update(QualifiedIndexingMessage message, TodoList<T> todoList) {
        delete(message.getOriginalMessage().getInstanceReference(), todoList);
        insert(message, todoList);
        return todoList;
    }

    @Override
    public <T> TodoList<T> delete(NexusInstanceReference reference, TodoList<T> todoList) {
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
