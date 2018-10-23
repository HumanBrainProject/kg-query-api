package org.humanbrainproject.knowledgegraph.indexing.control.inference;

import deprecated.control.Constants;
import org.humanbrainproject.knowledgegraph.indexing.control.ExecutionPlanner;
import org.humanbrainproject.knowledgegraph.indexing.control.IndexingController;
import org.humanbrainproject.knowledgegraph.indexing.control.IndexingProvider;
import org.humanbrainproject.knowledgegraph.indexing.control.MessageProcessor;
import org.humanbrainproject.knowledgegraph.indexing.entity.*;
import org.humanbrainproject.knowledgegraph.propertyGraph.entity.MainVertex;
import org.humanbrainproject.knowledgegraph.propertyGraph.entity.ResolvedVertexStructure;
import org.humanbrainproject.knowledgegraph.propertyGraph.entity.SubSpace;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Component
public class InferenceController implements IndexingController{

    public final static String INFERRED_BASE = Constants.HBP_BASE + "inference/";
    public final static String INFERRED_TYPE = INFERRED_BASE + "Inferred";
    public final static String INFERRED_SOURCE = INFERRED_BASE + "source";
    public final static String ORIGINAL_PARENT_PROPERTY = "http://hbp.eu/reconciled#original_parent";

    @Autowired
    MessageProcessor messageProcessor;

    @Autowired
    ExecutionPlanner executionPlanner;

    @Autowired
    IndexingProvider indexingProvider;

    private Set<InferenceStrategy> strategies = Collections.synchronizedSet(new HashSet<>());


    @Override
    public void insert(QualifiedIndexingMessage message, TodoList todoList){
        if(message.isOfType(INFERRED_TYPE)){
            ResolvedVertexStructure vertexStructureAlignedToMain = messageProcessor.createVertexStructureInAlternativeSpace(message, SubSpace.MAIN);
            indexingProvider.mapToOriginalSpace(vertexStructureAlignedToMain.getMainVertex());
            executionPlanner.addVertexWithEmbeddedInstancesToTodoList(todoList, vertexStructureAlignedToMain.getMainVertex(), indexingProvider.getConnection(TargetDatabase.INFERRED), TodoItem.Action.INSERT);
        } else {
            Set<MainVertex> documents = new HashSet<>();
            for (InferenceStrategy strategy : strategies) {
                strategy.infer(message, documents);
            }
            if(documents.isEmpty()){
                ResolvedVertexStructure vertexStructure = messageProcessor.createVertexStructureInAlternativeSpace(message, SubSpace.MAIN);
                indexingProvider.mapToOriginalSpace(vertexStructure.getMainVertex());
                executionPlanner.addVertexWithEmbeddedInstancesToTodoList(todoList, vertexStructure.getMainVertex(), indexingProvider.getConnection(TargetDatabase.INFERRED), TodoItem.Action.INSERT);
            }
            else{
                documents.forEach(doc -> executionPlanner.addVertexOrEdgeToTodoList(todoList, doc, null, TodoItem.Action.INSERT_OR_UPDATE_IN_PRIMARY_STORE));
            }
        }
    }

    @Override
    public void update(QualifiedIndexingMessage message, TodoList todoList) {
        delete(message.getOriginalMessage().getInstanceReference(), todoList);
        insert(message, todoList);
    }

    @Override
    public void delete(InstanceReference reference, TodoList todoList) {
        InstanceReference originalIdInMainSpace = indexingProvider.findOriginalId(reference).toSubSpace(SubSpace.MAIN);
        MainVertex inferredData = indexingProvider.getVertexStructureById(originalIdInMainSpace, TargetDatabase.INFERRED);
        if(inferredData!=null) {
            executionPlanner.removeVerticesAndEmbeddedEdges(todoList, inferredData, indexingProvider.getConnection(TargetDatabase.INFERRED));
        }
    }

    @Override
    public void clear() {
        indexingProvider.getConnection(TargetDatabase.INFERRED).clearData();
    }

    void addInferenceStrategy(InferenceStrategy strategy) {
        strategies.add(strategy);
    }

}
