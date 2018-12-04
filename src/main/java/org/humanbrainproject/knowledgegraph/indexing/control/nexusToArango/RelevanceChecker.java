package org.humanbrainproject.knowledgegraph.indexing.control.nexusToArango;

import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.ArangoRepository;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoDocumentReference;
import org.humanbrainproject.knowledgegraph.indexing.entity.QualifiedIndexingMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class RelevanceChecker {

    @Autowired
    ArangoRepository repository;

    public boolean isMessageRelevant(QualifiedIndexingMessage message) {
        Integer nexusRevision = message.getNexusRevision();
        if(nexusRevision!=null){
            Integer currentRevision = repository.getCurrentRevision(ArangoDocumentReference.fromNexusInstance(message.getOriginalId()));
            return currentRevision <= nexusRevision;
        }
        return true;
    }

}
