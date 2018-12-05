package org.humanbrainproject.knowledgegraph.indexing.control.nexusToArango;

import org.humanbrainproject.knowledgegraph.commons.authorization.entity.InternalMasterKey;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.ArangoDatabaseFactory;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.ArangoRepository;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoDocumentReference;
import org.humanbrainproject.knowledgegraph.indexing.entity.QualifiedIndexingMessage;
import org.humanbrainproject.knowledgegraph.query.entity.JsonDocument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class RelevanceChecker {

    @Autowired
    ArangoRepository repository;

    @Autowired
    ArangoDatabaseFactory databaseFactory;

    public boolean isMessageRelevant(QualifiedIndexingMessage message) {
        Map document = repository.getDocument(ArangoDocumentReference.fromNexusInstance(message.getOriginalId()), databaseFactory.getDefaultDB(), new InternalMasterKey());
        if (document != null) {
            JsonDocument doc = new JsonDocument(document);
            Integer existingNexusRevision = doc.getNexusRevision();
            String existingNexusId = doc.getNexusId();
            //Integer nexusRevision = message.getNexusRevision();
            if (message.getOriginalMessage().getInstanceReference().getId() == null || !message.getOriginalMessage().getInstanceReference().getId().equals(existingNexusId)) {
                return true;
            }
            return message.getNexusRevision() == null || existingNexusRevision == null || message.getNexusRevision() >= existingNexusRevision;
        }
        return true;
    }

}
