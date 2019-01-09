package org.humanbrainproject.knowledgegraph.indexing.entity.knownSemantics;

import org.humanbrainproject.knowledgegraph.annotations.ToBeTested;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.exceptions.UnexpectedNumberOfResults;
import org.humanbrainproject.knowledgegraph.commons.vocabulary.HBPVocabulary;
import org.humanbrainproject.knowledgegraph.commons.vocabulary.SchemaOrgVocabulary;
import org.humanbrainproject.knowledgegraph.indexing.entity.QualifiedIndexingMessage;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusInstanceReference;

import java.util.List;

@ToBeTested(easy = true)
public class LinkingInstance extends KnownSemantic {

    public LinkingInstance(QualifiedIndexingMessage spec) {
        super(spec, HBPVocabulary.LINKING_INSTANCE_TYPE);
    }

    public NexusInstanceReference getFrom(){
        List<NexusInstanceReference> referencesForLinkedInstances = getReferencesForLinkedInstances(HBPVocabulary.LINKING_INSTANCE_FROM, true);
        return getSingleResult(referencesForLinkedInstances, "A linking instance should only contain a single from");
    }

    private NexusInstanceReference getSingleResult(List<NexusInstanceReference> referencesForLinkedInstances, String errorMessage) {
        if (referencesForLinkedInstances != null && !referencesForLinkedInstances.isEmpty()) {
            if (referencesForLinkedInstances.size() == 1) {
                return referencesForLinkedInstances.get(0);
            } else {
                throw new UnexpectedNumberOfResults(errorMessage);
            }
        } else {
            return null;
        }
    }

    public NexusInstanceReference getTo(){
        List<NexusInstanceReference> referencesForLinkedInstances = getReferencesForLinkedInstances(HBPVocabulary.LINKING_INSTANCE_TO, true);
        return getSingleResult(referencesForLinkedInstances, "A linking instance should only contain a single to");
    }

    public String getName(){
        return (String)spec.getQualifiedMap().get(SchemaOrgVocabulary.NAME);
    }


}
