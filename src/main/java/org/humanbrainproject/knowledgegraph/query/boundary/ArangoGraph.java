package org.humanbrainproject.knowledgegraph.query.boundary;

import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.ArangoDatabaseFactory;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.ArangoRepository;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoCollectionReference;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoDocumentReference;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusInstanceReference;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusSchemaReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class ArangoGraph {

    @Autowired
    ArangoDatabaseFactory databaseFactory;

    @Autowired
    ArangoRepository arangoRepository;

    public List<Map> getGraph(NexusInstanceReference instance, Integer step) {
        return arangoRepository.inDepthGraph(ArangoDocumentReference.fromNexusInstance(instance), step, databaseFactory.getDefaultDB());
    }

    public List<Map> getGetEditorSpecDocument(ArangoCollectionReference collection) {
        return  arangoRepository.getGetEditorSpecDocument(collection, databaseFactory.getInternalDB());
    }

    public Map getInstanceList(NexusSchemaReference schemaReference, Integer from, Integer size, String searchTerm){
        return arangoRepository.getInstanceList(ArangoCollectionReference.fromNexusSchemaReference(schemaReference), from, size, searchTerm ,databaseFactory.getInferredDB());
    }

    public List<Map> getInstance(ArangoDocumentReference instanceReference){
        return arangoRepository.getInstance(instanceReference, databaseFactory.getInferredDB());
    }

    public Map getBookmarks(NexusInstanceReference instanceRef,Integer from, Integer size, String searchTerm ){
        return arangoRepository.getBookmarks(instanceRef, from, size, searchTerm ,databaseFactory.getInferredDB());
    }
}
