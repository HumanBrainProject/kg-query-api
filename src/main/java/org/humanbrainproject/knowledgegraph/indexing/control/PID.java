package org.humanbrainproject.knowledgegraph.indexing.control;

import org.humanbrainproject.knowledgegraph.annotations.ToBeTested;
import org.humanbrainproject.knowledgegraph.commons.jsonld.control.JsonTransformer;
import org.humanbrainproject.knowledgegraph.commons.nexus.control.NexusConfiguration;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.ArangoDatabaseFactory;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.ArangoRepository;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.entity.Vertex;
import org.humanbrainproject.knowledgegraph.commons.vocabulary.HBPVocabulary;
import org.humanbrainproject.knowledgegraph.commons.vocabulary.SchemaOrgVocabulary;
import org.humanbrainproject.knowledgegraph.indexing.entity.IndexingMessage;
import org.humanbrainproject.knowledgegraph.indexing.entity.QualifiedIndexingMessage;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusInstanceReference;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusSchemaReference;
import org.humanbrainproject.knowledgegraph.indexing.entity.todo.InsertOrUpdateInPrimaryStoreTodoItem;
import org.humanbrainproject.knowledgegraph.indexing.entity.todo.TodoList;
import org.humanbrainproject.knowledgegraph.query.entity.JsonDocument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@ToBeTested
@Component
public class PID {

    @Autowired
    MessageProcessor messageProcessor;

    @Autowired
    ArangoDatabaseFactory databaseFactory;

    @Autowired
    ArangoRepository repository;

    @Autowired
    JsonTransformer jsonTransformer;

    @Autowired
    NexusConfiguration nexusConfiguration;


    public static NexusSchemaReference createNexusSchemaReference(String type){
        return new NexusSchemaReference("global", "pid", type, "v1.0.0");
    }


    public void createOrUpdatePid(QualifiedIndexingMessage indexingMessage, TodoList todoList){
        if(indexingMessage!=null){
            Set<String> types = indexingMessage.getTypes();
            Set<String> identifiers = indexingMessage.getIdentifiers();
            for (String type : types) {
                createOrUpdatePidByType(indexingMessage.getOriginalMessage().getInstanceReference(), type, identifiers, todoList);
            }
        }
    }

    private void createOrUpdatePidByType(NexusInstanceReference instanceReference, String type, Set<String> identifiers, TodoList todoList){
        Map pid = repository.findPid(type, identifiers, databaseFactory.getInferredDB());
        JsonDocument jsonDocument;
        NexusSchemaReference schemaReferenceByType = createNexusSchemaReference(type);
        if(pid==null){
            jsonDocument = new JsonDocument();
            String staticId = identifiers.isEmpty() ? instanceReference.getId() : identifiers.iterator().next();
            jsonDocument.put(HBPVocabulary.PID, staticId);
            jsonDocument.put(SchemaOrgVocabulary.IDENTIFIER, identifiers);
            jsonDocument.addType(schemaReferenceByType.getType());
        }
        else{
            jsonDocument = new JsonDocument(pid);
            Set<String> mergedIdentifiers = new HashSet<>(identifiers);
            mergedIdentifiers.addAll((Collection<String>)  pid.get(SchemaOrgVocabulary.IDENTIFIER));
            jsonDocument.put(SchemaOrgVocabulary.IDENTIFIER, mergedIdentifiers);
        }
        jsonDocument.addReference(HBPVocabulary.INFERENCE_EXTENDED_BY, nexusConfiguration.getAbsoluteUrl(instanceReference));
        IndexingMessage indexingMessage = new IndexingMessage(new NexusInstanceReference(schemaReferenceByType, null), jsonTransformer.getMapAsJson(jsonDocument), null, null);
        Vertex vertex = messageProcessor.createVertexStructure(messageProcessor.qualify(indexingMessage));
        todoList.addTodoItem(new InsertOrUpdateInPrimaryStoreTodoItem(vertex));
    }

}
