package org.humanbrainproject.knowledgegraph.indexing.entity.knownSemantics;

import com.github.jsonldjava.core.JsonLdConsts;
import org.humanbrainproject.knowledgegraph.commons.vocabulary.HBPVocabulary;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusInstanceReference;
import org.humanbrainproject.knowledgegraph.indexing.entity.KnownSemantic;
import org.humanbrainproject.knowledgegraph.indexing.entity.QualifiedIndexingMessage;

import java.util.Map;

public class Release extends KnownSemantic {



    public Release(QualifiedIndexingMessage spec) {
        super(spec, HBPVocabulary.RELEASE_TYPE);
    }

    public NexusInstanceReference getReleaseInstance(){
        Object releaseInstance = this.spec.getQualifiedMap().get(HBPVocabulary.RELEASE_INSTANCE);
        if(releaseInstance instanceof Map && ((Map)releaseInstance).containsKey(JsonLdConsts.ID)){
            NexusInstanceReference reference = NexusInstanceReference.createFromUrl((String) ((Map) releaseInstance).get(JsonLdConsts.ID));
            Number revision = (Number)this.spec.getQualifiedMap().get(HBPVocabulary.RELEASE_REVISION);
            if(revision!=null && reference!=null) {
                reference.setRevision(revision.intValue());
            }
            return reference;
        }
        return null;
    }


}
