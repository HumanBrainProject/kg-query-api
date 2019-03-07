package org.humanbrainproject.knowledgegraph.indexing.control.suggestion;

import org.humanbrainproject.knowledgegraph.annotations.ToBeTested;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.entity.SubSpace;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.entity.Vertex;
import org.humanbrainproject.knowledgegraph.indexing.control.IndexingController;
import org.humanbrainproject.knowledgegraph.indexing.control.MessageProcessor;
import org.humanbrainproject.knowledgegraph.indexing.control.inference.InferenceStrategy;
import org.humanbrainproject.knowledgegraph.indexing.control.nexusToArango.NexusToArangoIndexingProvider;
import org.humanbrainproject.knowledgegraph.indexing.entity.QualifiedIndexingMessage;
import org.humanbrainproject.knowledgegraph.indexing.entity.TargetDatabase;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusInstanceReference;
import org.humanbrainproject.knowledgegraph.indexing.entity.todo.DeleteTodoItem;
import org.humanbrainproject.knowledgegraph.indexing.entity.todo.InsertOrUpdateInPrimaryStoreTodoItem;
import org.humanbrainproject.knowledgegraph.indexing.entity.todo.InsertTodoItem;
import org.humanbrainproject.knowledgegraph.indexing.entity.todo.TodoList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * The inference controller applies the registered inference strategies and registers the created / updated / deleted instances into the inferred database.
 */
@Component
@ToBeTested
public class SuggestionInferenceController implements IndexingController {

    @Autowired
    NexusToArangoIndexingProvider indexingProvider;

    @Autowired
    MessageProcessor messageProcessor;

    private Set<InferenceStrategy> strategies = Collections.synchronizedSet(new HashSet<>());

    @Override
    public TodoList insert(QualifiedIndexingMessage message, TodoList todoList) {
        if(message.getOriginalMessage().getInstanceReference().getSubspace().equals(SubSpace.SUGGESTION)){
            Vertex vertex = messageProcessor.createVertexStructure(message);
            todoList.addTodoItem(new InsertTodoItem(vertex, indexingProvider.getConnection(TargetDatabase.INFERRED)));
        }
        return todoList;
    }

    @Override
    public TodoList update(QualifiedIndexingMessage message, TodoList todoList) {
        return insert(message, todoList);
    }

    @Override
    public TodoList delete(NexusInstanceReference reference, TodoList todoList) {
        if(reference.getSubspace().equals(SubSpace.SUGGESTION)){
            todoList.addTodoItem(new DeleteTodoItem(reference, indexingProvider.getConnection(TargetDatabase.INFERRED)));
            todoList.addTodoItem(new DeleteTodoItem(reference, indexingProvider.getConnection(TargetDatabase.NATIVE)));
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
